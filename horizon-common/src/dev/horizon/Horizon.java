package dev.horizon;

import static java.lang.invoke.MethodHandles.lookup;
import static java.lang.invoke.MethodHandles.privateLookupIn;
import static java.lang.reflect.Modifier.isPublic;
import static java.nio.ByteBuffer.wrap;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.stream;
import static java.util.Collections.emptySet;
import static java.util.Map.entry;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import dev.horizon.cache.HorizonCache;
import dev.horizon.codec.HorizonCodec;
import dev.horizon.lock.DistributedLock;
import dev.horizon.packet.Packet;
import dev.horizon.packet.PacketCallbackCache;
import dev.horizon.packet.PacketCallbackException;
import dev.horizon.packet.PacketCallbackListener;
import dev.horizon.packet.PacketException;
import dev.horizon.packet.PacketHandler;
import dev.horizon.packet.PacketListener;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import java.io.Closeable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class Horizon implements Closeable {
  private static final RedisCodec<String, byte[]> BINARY_CODEC = new RedisBinaryCodec();
  private static final MethodHandles.Lookup LOOKUP = lookup();

  private final RedisClient redisClient;
  private final HorizonCodec codec;
  private final Duration requestCleanupInterval;

  private final StatefulRedisConnection<String, byte[]> connection;
  private final StatefulRedisPubSubConnection<String, byte[]> pubSubConnection;

  private final PacketCallbackCache packetCallbackCache;

  private final Set<String> subscribedTopics;
  private final Map<SubscriptionCompositeKey, Set<Map.Entry<Object, Set<MethodHandle>>>>
      subscriptionsByPacketType;

  private final Map<String, HorizonCache> cacheByKey;
  private final Map<String, DistributedLock> lockByName;

  Horizon(
      final RedisClient redisClient,
      final HorizonCodec codec,
      final Duration requestCleanupInterval) {
    this.redisClient = redisClient;
    this.codec = codec;
    this.requestCleanupInterval = requestCleanupInterval;

    this.connection = redisClient.connect(BINARY_CODEC);
    this.pubSubConnection = redisClient.connectPubSub(BINARY_CODEC);

    this.subscribedTopics = ConcurrentHashMap.newKeySet();
    subscribedTopics.add("callbacks");
    pubSubConnection.sync().subscribe("callbacks");
    this.packetCallbackCache = new PacketCallbackCache();
    pubSubConnection.addListener(
        PacketListener.create("callbacks", new PacketCallbackListener(codec, packetCallbackCache)));

    this.subscriptionsByPacketType = new ConcurrentHashMap<>();
    this.cacheByKey = new ConcurrentHashMap<>();
    this.lockByName = new ConcurrentHashMap<>();
  }

  public <T extends Packet> CompletableFuture<Void> publish(
      final String channelName, final T packet) {
    try {
      final byte[] payload = codec.encode(packet);
      return connection
          .async()
          .publish(channelName, payload)
          .exceptionally(
              cause -> {
                throw new HorizonException(
                    "Could not publish a packet, because of unexpected exception.", cause);
              })
          .thenAccept(v -> {})
          .toCompletableFuture();
    } catch (final Exception exception) {
      throw new HorizonException(
          "Could not publish packet over the packet broker, because of unexpected exception.",
          exception);
    }
  }

  public void subscribe(final String topic, final Object subscriber) throws HorizonException {
    if (topic == null || topic.isEmpty()) {
      throw new HorizonException(
          "%s's identity cannot be null or empty".formatted(subscriber.getClass()));
    }

    final Set<Class<? extends Packet>> packetTypes = subscribe0(topic, subscriber);
    pubSubConnection.addListener(createPacketListener(topic, packetTypes));
    if (subscribedTopics.contains(topic)) {
      return;
    }

    subscribedTopics.add(topic);
    pubSubConnection.sync().subscribe(topic);
  }

  private PacketListener createPacketListener(
      final String topic, final Collection<Class<? extends Packet>> packetTypes) {
    return PacketListener.create(
        topic,
        (channelName, payload) -> {
          final Packet packet = codec.decode(payload, Packet.class);
          if (packet == null) {
            return;
          }

          if (packetTypes.contains(packet.getClass())) {
            subscriptionsByPacketType
                .getOrDefault(new SubscriptionCompositeKey(packet.getClass(), topic), emptySet())
                .stream()
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue))
                .forEach(
                    (subscriber, invocations) -> {
                      for (final MethodHandle invocation : invocations) {
                        try {
                          final Object returnedValue = invocation.invoke(subscriber, packet);
                          if (returnedValue != null) {
                            processReturnValue(returnedValue);
                          }
                        } catch (final Throwable exception) {
                          throw new PacketException(
                              "Could not publish event, because of unexpected exception during method invocation.",
                              exception);
                        }
                      }
                    });
          }
        });
  }

  private void processReturnValue(final Object returnedValue) {
    if (returnedValue == null) {
      return;
    }

    final Class<?> resultType = returnedValue.getClass();
    if (resultType == CompletableFuture.class) {
      ((CompletableFuture<?>) returnedValue)
          .whenComplete((result, cause) -> processReturnValue(result))
          .exceptionally(
              cause -> {
                throw new PacketException(
                    "Could not handle result of type %s, because of an exception."
                        .formatted(cause.getClass().getName()),
                    cause);
              });
      return;
    }

    publish("callbacks", ((Packet) returnedValue));
  }

  public <T extends Packet> CompletableFuture<T> request(
      final String channelName, final Packet packet) {
    final UUID uniqueId = packet.getUniqueId();
    try {
      final CompletableFuture<T> completableFuture = new CompletableFuture<>();
      packetCallbackCache.add(uniqueId, completableFuture);

      publish(channelName, packet);

      return completableFuture
          .orTimeout(requestCleanupInterval.toMillis(), MILLISECONDS)
          .exceptionally(
              throwable -> {
                packetCallbackCache.remove(uniqueId);
                throw new PacketCallbackException(
                    "Failed to request packet identified by %s".formatted(uniqueId), throwable);
              });
    } catch (final Exception exception) {
      throw new PacketCallbackException(
          "Could not request packet over the packet broker, because of unexpected exception.",
          exception);
    }
  }

  public HorizonCache getCache(final String key) throws HorizonException {
    return cacheByKey.computeIfAbsent(key, k -> new HorizonCache(k, this));
  }

  public DistributedLock getDistributedLock(final String key) throws HorizonException {
    return lockByName.computeIfAbsent(
        key,
        k -> {
          final HorizonCache lockCache = getCache("locks");
          return new DistributedLock(k, lockCache);
        });
  }

  @Override
  public void close() {
    redisClient.close();
    connection.close();
    pubSubConnection.close();
    subscribedTopics.clear();
  }

  @SuppressWarnings("unchecked")
  private Set<Class<? extends Packet>> subscribe0(final String topic, final Object subscriber)
      throws HorizonException {

    final Class<?> subscriberType = subscriber.getClass();
    final Map<Class<? extends Packet>, Set<MethodHandle>> methodHandlesByPacketType =
        stream(subscriberType.getDeclaredMethods())
            .filter(
                method ->
                    method.isAnnotationPresent(PacketHandler.class)
                        && Packet.class.isAssignableFrom(method.getParameterTypes()[0]))
            .map(method -> getMethodHandle(subscriberType, method))
            .collect(
                groupingBy(
                    method -> (Class<? extends Packet>) method.type().lastParameterType(),
                    toSet()));

    methodHandlesByPacketType.forEach(
        (packetType, methodHandles) ->
            subscriptionsByPacketType
                .computeIfAbsent(
                    new SubscriptionCompositeKey(packetType, topic), key -> new HashSet<>())
                .add(entry(subscriber, methodHandles)));

    return methodHandlesByPacketType.keySet();
  }

  private MethodHandle getMethodHandle(final Class<?> type, final Method method) {
    try {
      final MethodHandles.Lookup lookup =
          isPublic(type.getModifiers()) ? LOOKUP : privateLookupIn(type, LOOKUP);
      return lookup.unreflect(method);
    } catch (final Exception exception) {
      throw new PacketException(
          "Could not get method handle for %s method, at %s.".formatted(method.getName(), type),
          exception);
    }
  }

  public RedisClient redisClient() {
    return redisClient;
  }

  public StatefulRedisConnection<String, byte[]> connection() {
    return connection;
  }

  public StatefulRedisPubSubConnection<String, byte[]> pubSubConnection() {
    return pubSubConnection;
  }

  public HorizonCodec codec() {
    return codec;
  }

  public PacketCallbackCache packetCallbackCache() {
    return packetCallbackCache;
  }

  public Duration requestCleanupInterval() {
    return requestCleanupInterval;
  }

  public Set<String> subscribedTopics() {
    return subscribedTopics;
  }

  public Map<String, HorizonCache> cacheByName() {
    return cacheByKey;
  }

  public Map<String, DistributedLock> lockByName() {
    return lockByName;
  }

  private record SubscriptionCompositeKey(Class<? extends Packet> packetType, String topic) {}

  private static final class RedisBinaryCodec implements RedisCodec<String, byte[]> {

    @Override
    public String decodeKey(final ByteBuffer buffer) {
      return UTF_8.decode(buffer).toString();
    }

    @Override
    public byte[] decodeValue(final ByteBuffer buffer) {
      final byte[] bytes = new byte[buffer.remaining()];
      buffer.get(bytes);
      return bytes;
    }

    @Override
    public ByteBuffer encodeKey(final String value) {
      return wrap(value.getBytes(UTF_8));
    }

    @Override
    public ByteBuffer encodeValue(final byte[] value) {
      return wrap(value);
    }
  }
}
