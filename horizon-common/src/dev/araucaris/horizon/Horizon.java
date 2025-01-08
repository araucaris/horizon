package dev.araucaris.horizon;

import static java.nio.ByteBuffer.wrap;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.stream;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import dev.araucaris.horizon.distributed.DistributedLock;
import dev.araucaris.horizon.packet.Packet;
import dev.araucaris.horizon.packet.PacketListener;
import dev.araucaris.horizon.packet.callback.PacketCallbackCache;
import dev.araucaris.horizon.packet.callback.PacketCallbackException;
import dev.araucaris.horizon.packet.callback.PacketCallbackListener;
import dev.araucaris.horizon.serdes.HorizonSerdes;
import dev.araucaris.horizon.storage.HorizonStorage;
import dev.shiza.dew.event.EventBus;
import dev.shiza.dew.subscription.Subscribe;
import dev.shiza.dew.subscription.Subscriber;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class Horizon {
  private static final RedisBinaryCodec BINARY_CODEC = new RedisBinaryCodec();

  private final RedisClient redisClient;
  private final StatefulRedisConnection<String, byte[]> connection;
  private final StatefulRedisPubSubConnection<String, byte[]> pubSubConnection;
  private final EventBus eventBus;
  private final HorizonSerdes horizonSerdes;
  private final PacketCallbackCache packetCallbackCache = PacketCallbackCache.create();
  private final Duration requestCleanupInterval;
  private final Set<String> subscribedTopics = ConcurrentHashMap.newKeySet();
  private final Map<String, HorizonStorage> storageByName = new ConcurrentHashMap<>();

  Horizon(
      RedisClient redisClient,
      EventBus eventBus,
      HorizonSerdes horizonSerdes,
      Duration requestCleanupInterval) {
    subscribedTopics.add("callbacks");

    // Redis
    this.redisClient = redisClient;
    this.connection = redisClient.connect(BINARY_CODEC);
    this.pubSubConnection = redisClient.connectPubSub(BINARY_CODEC);
    pubSubConnection.sync().subscribe("callbacks");
    pubSubConnection.addListener(
        PacketListener.create(
            "callbacks", PacketCallbackListener.create(horizonSerdes, packetCallbackCache)));

    // EventBus
    this.eventBus = eventBus;
    this.eventBus.result(Packet.class, (event, packet) -> publish("callbacks", packet));

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

  public void subscribe(Subscriber subscriber) throws HorizonException {
    String identity = subscriber.identity();
    if (identity == null || identity.isEmpty()) {
      throw new HorizonException("Subscriber's identity cannot be null or empty");
    }

    eventBus.subscribe(subscriber);

    Set<Class<? extends Packet>> packetTypes = new HashSet<>();
    for (Method method : subscriber.getClass().getDeclaredMethods()) {
      if (!method.isAnnotationPresent(Subscribe.class)) {
        continue;
      }

      if (method.getParameterCount() == 0) {
        throw new HorizonException(
            "Subscriber's method %s parameter count is zero".formatted(method.getName()));
      }

      //noinspection unchecked
      Class<? extends Packet> packetType =
          (Class<? extends Packet>)
              stream(method.getParameterTypes())
                  .filter(Packet.class::isAssignableFrom)
                  .findAny()
                  .orElseThrow(
                      () ->
                          new NullPointerException(
                              "No valid message type found under %s#%s"
                                  .formatted(subscriber.getClass(), method)));
      packetTypes.add(packetType);
    }

    pubSubConnection.addListener(
        PacketListener.create(
            identity,
            (channelName, payload) -> {
              Packet packet = horizonSerdes.decode(payload, Packet.class);
              if (packet == null) {
                return;
              }

              boolean whetherListensForPacket = packetTypes.contains(packet.getClass());
              if (whetherListensForPacket) {
                eventBus.publish(packet, identity);
              }
            }));

    if (subscribedTopics.contains(identity)) {
      return;
    }

    subscribedTopics.add(identity);
    pubSubConnection.sync().subscribe(identity);
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

  public HorizonStorage getStorage(String name) throws HorizonException {
    return storageByName.computeIfAbsent(
        name, k -> HorizonStorage.create(name, horizonSerdes, connection));
  }

  public DistributedLock getLock(String key) {
    return new DistributedLock(key, getStorage("locks"));
  }

  public void close() throws IOException {
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
