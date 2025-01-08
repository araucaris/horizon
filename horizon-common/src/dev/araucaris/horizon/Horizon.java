package dev.araucaris.horizon;

import static java.nio.ByteBuffer.wrap;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.toMap;

import dev.araucaris.horizon.distributed.DistributedLock;
import dev.araucaris.horizon.packet.Packet;
import dev.araucaris.horizon.packet.PacketException;
import dev.araucaris.horizon.packet.PacketListener;
import dev.araucaris.horizon.packet.callback.PacketCallbackCache;
import dev.araucaris.horizon.packet.callback.PacketCallbackException;
import dev.araucaris.horizon.packet.callback.PacketCallbackListener;
import dev.araucaris.horizon.serdes.HorizonSerdes;
import dev.araucaris.horizon.storage.HorizonStorage;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import java.io.Closeable;
import java.lang.invoke.MethodHandle;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class Horizon implements Closeable {
  private static final RedisBinaryCodec BINARY_CODEC = new RedisBinaryCodec();

  private final RedisClient redisClient;
  private final StatefulRedisConnection<String, byte[]> connection;
  private final StatefulRedisPubSubConnection<String, byte[]> pubSubConnection;
  private final SubscriptionService subscriptionService = SubscriptionService.create();
  private final HorizonSerdes horizonSerdes;
  private final PacketCallbackCache packetCallbackCache = PacketCallbackCache.create();
  private final Duration requestCleanupInterval;
  private final Set<String> subscribedTopics = ConcurrentHashMap.newKeySet();
  private final Map<String, HorizonStorage> storageByName = new ConcurrentHashMap<>();

  Horizon(RedisClient redisClient, HorizonSerdes horizonSerdes, Duration requestCleanupInterval) {
    subscribedTopics.add("callbacks");

    // Redis
    this.redisClient = redisClient;
    this.connection = redisClient.connect(BINARY_CODEC);
    this.pubSubConnection = redisClient.connectPubSub(BINARY_CODEC);
    pubSubConnection.sync().subscribe("callbacks");
    pubSubConnection.addListener(
        PacketListener.create(
            "callbacks", PacketCallbackListener.create(horizonSerdes, packetCallbackCache)));

    this.horizonSerdes = horizonSerdes;
    this.requestCleanupInterval = requestCleanupInterval;
  }

  public static HorizonBuilder newBuilder(RedisClient redisClient) {
    return HorizonBuilder.newBuilder(redisClient);
  }

  public <T extends Packet> CompletableFuture<Void> publish(String channelName, T packet) {
    try {
      byte[] payload = horizonSerdes.encode(packet);
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
    } catch (Exception exception) {
      throw new HorizonException(
          "Could not publish packet over the packet broker, because of unexpected exception.",
          exception);
    }
  }

  public void subscribe(String topic, Object subscriber) throws HorizonException {
    if (topic == null || topic.isEmpty()) {
      throw new HorizonException(
          "%s's identity cannot be null or empty".formatted(subscriber.getClass()));
    }

    Set<Class<? extends Packet>> packetTypes = subscriptionService.subscribe(topic, subscriber);
    pubSubConnection.addListener(createPacketListener(topic, packetTypes));
    if (subscribedTopics.contains(topic)) {
      return;
    }

    subscribedTopics.add(topic);
    pubSubConnection.sync().subscribe(topic);
  }

  private PacketListener createPacketListener(
      String topic, Set<Class<? extends Packet>> packetTypes) {
    return PacketListener.create(
        topic,
        (channelName, payload) -> {
          Packet packet = horizonSerdes.decode(payload, Packet.class);
          if (packet == null) {
            return;
          }

          if (packetTypes.contains(packet.getClass())) {
            subscriptionService.retrieveByPacketTypeAndTopic(packet.getClass(), topic).stream()
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue))
                .forEach(
                    (subscriber, invocations) -> {
                      for (MethodHandle invocation : invocations) {
                        try {
                          Object returnedValue = invocation.invoke(subscriber, packet);
                          if (returnedValue != null) {
                            processReturnValue(returnedValue);
                          }
                        } catch (Throwable exception) {
                          throw new PacketException(
                              "Could not publish event, because of unexpected exception during method invocation.",
                              exception);
                        }
                      }
                    });
          }
        });
  }

  private void processReturnValue(Object returnedValue) {
    if (returnedValue == null) {
      return;
    }

    Class<?> resultType = returnedValue.getClass();
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

  public <T extends Packet> CompletableFuture<T> request(String channelName, Packet packet) {
    UUID uniqueId = packet.getUniqueId();
    try {
      CompletableFuture<T> completableFuture = new CompletableFuture<>();
      publish(channelName, packet);
      packetCallbackCache.add(uniqueId, completableFuture);
      return completableFuture
          .orTimeout(requestCleanupInterval.toMillis(), MILLISECONDS)
          .exceptionally(
              throwable -> {
                packetCallbackCache.remove(uniqueId);
                throw new PacketCallbackException(
                    "Failed to request packet identified by %s".formatted(uniqueId), throwable);
              });
    } catch (Exception exception) {
      throw new PacketCallbackException(
          "Could not request packet over the packet broker, because of unexpected exception.",
          exception);
    }
  }

  public HorizonStorage retrieveStorage(String name) throws HorizonException {
    return storageByName.computeIfAbsent(
        name, k -> HorizonStorage.create(name, horizonSerdes, connection));
  }

  public DistributedLock retrieveLock(String key) {
    return new DistributedLock(key, retrieveStorage("locks"));
  }

  @Override
  public void close() {
    redisClient.close();
    connection.close();
    pubSubConnection.close();
    subscribedTopics.clear();
  }

  static final class RedisBinaryCodec implements RedisCodec<String, byte[]> {

    private RedisBinaryCodec() {}

    @Override
    public String decodeKey(ByteBuffer buffer) {
      return UTF_8.decode(buffer).toString();
    }

    @Override
    public byte[] decodeValue(ByteBuffer buffer) {
      byte[] bytes = new byte[buffer.remaining()];
      buffer.get(bytes);
      return bytes;
    }

    @Override
    public ByteBuffer encodeKey(String value) {
      return wrap(value.getBytes(UTF_8));
    }

    @Override
    public ByteBuffer encodeValue(byte[] value) {
      return wrap(value);
    }
  }
}
