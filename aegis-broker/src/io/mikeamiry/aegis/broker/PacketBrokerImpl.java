package io.mikeamiry.aegis.broker;

import static java.util.UUID.randomUUID;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.makeamiry.aegis.codec.Codec;
import io.mikeamiry.aegis.eventbus.EventBus;
import io.mikeamiry.aegis.eventbus.Observer;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * The PacketBroker class facilitates packet-based communication using a combination of an event bus
 * and Redis-based channels. It acts as a message broker handling packet encoding, decoding,
 * publishing, and subscribing mechanisms.
 *
 * <p>This class is final and cannot be subclassed. Communication involves sending and receiving
 * packets over defined channels, leveraging Redis pub/sub capabilities and an event bus.
 *
 * <p>Responsibilities of this class include: - Encoding and decoding packets using a provided
 * Codec. - Subscribing to events and packets via the EventBus and Redis subscriptions. - Publishing
 * packets to Redis channels. - Managing callbacks for packet responses via CompletableFutures. -
 * Delegating packets to an EventBus or handling them internally.
 *
 * <p>Features: - The broker ensures every request packet has a unique source identifier. - It
 * supports asynchronous requests, returning a CompletableFuture to handle responses. - Observers
 * can subscribe to specific topics for receiving packets. - Gracefully handles any exceptions that
 * arise from publishing, subscribing, or delegation.
 *
 * <p>Internal mechanisms include topics and callbacks management: - Topics are tracked using a
 * thread-safe set to ensure unique subscriptions. - Callbacks map response packets to their
 * corresponding CompletableFuture for asynchronous processing.
 *
 * <p>Typical operations supported by this class: - Publishing a packet to a specific channel. -
 * Observing event or packet channels. - Sending an asynchronous request and waiting for a response.
 *
 * <p>This class uses the following dependencies: - Codec for encoding and decoding packets to/from
 * byte arrays. - EventBus for inter-component communication. - Lettuce Redis connections for
 * pub/sub communication.
 */
final class PacketBrokerImpl implements PacketBroker {

  private final Codec codec;
  private final String identity;
  private final EventBus eventBus;
  private final StatefulRedisConnection<String, byte[]> connection;
  private final StatefulRedisPubSubConnection<String, byte[]> pubSubConnection;
  private final Map<String, CompletableFuture<?>> callbacks;
  private final Set<String> observedTopics;

  PacketBrokerImpl(
      final String identity,
      final Codec codec,
      final EventBus eventBus,
      final RedisClient redisClient) {
    this.identity = identity;
    this.eventBus = eventBus;
    this.codec = codec;
    this.eventBus.register(Packet.class, this::delegateToPacketBroker);
    final RedisCodec<String, byte[]> stringByteCodec = new StringByteCodec();
    this.connection = redisClient.connect(stringByteCodec);
    this.pubSubConnection = redisClient.connectPubSub(stringByteCodec);
    this.callbacks = new ConcurrentHashMap<>();
    this.observedTopics = ConcurrentHashMap.newKeySet();
    observeCallbacks();
  }

  private void delegateToPacketBroker(final Packet request, final Packet response)
      throws PacketBrokerException {
    if (request.source() == null) {
      throw new PacketBrokerException(
          "Could not delegate packet to packet broker due to missing source.");
    }

    publish("callbacks", response);
  }

  private boolean observeCallbacks() {
    return observePacketBroker(
        "callbacks",
        message -> {
          final Packet response = codec.decodeFromBytes(message);
          if (response.target() == null) {
            return;
          }

          //noinspection unchecked
          Optional.of(callbacks.get(response.target()))
              .map(future -> (CompletableFuture<Packet>) future)
              .ifPresent(future -> future.complete(response));
        });
  }

  public void observe(final Observer observer) throws PacketBrokerException {
    observeEventBus(observer);
    observePacketBroker(observer);
  }

  public void publish(final String channel, final Packet packet) throws PacketBrokerException {
    try {
      if (packet.source() == null) {
        packet.source(identity);
      }
      connection.sync().publish(channel, codec.encodeToBytes(packet));
    } catch (final Exception exception) {
      throw new PacketBrokerException(
          "Could not publish packet on channel named %s due to unexpected exception."
              .formatted(channel),
          exception);
    }
  }

  public <T extends Packet> CompletableFuture<T> request(
      final String channel, final Packet request) {
    request.source(randomUUID().toString());

    final CompletableFuture<T> future = new CompletableFuture<>();
    callbacks.put(request.source(), future);

    publish(channel, request);
    return future;
  }

  private void delegateToEventBus(final String topic, final byte[] message)
      throws PacketBrokerException {
    final Packet packet = codec.decodeFromBytes(message);
    if (Objects.equals(packet.source(), identity)) {
      return;
    }
    try {
      eventBus.publish(packet, topic);
    } catch (final Exception exception) {
      throw new PacketBrokerException(
          "Could not delegate packet to event bus due to unexpected exception.", exception);
    }
  }

  private void observeEventBus(final Observer observer) throws PacketBrokerException {
    try {
      eventBus.observe(observer);
    } catch (final Exception exception) {
      throw new PacketBrokerException(
          "Could not subscribe to events on channel named %s due to unexpected exception."
              .formatted(observer.topic()),
          exception);
    }
  }

  private boolean observePacketBroker(final Observer observer) throws PacketBrokerException {
    final String topic = observer.topic();
    return observePacketBroker(topic, message -> delegateToEventBus(topic, message));
  }

  private boolean observePacketBroker(final String topic, final Consumer<byte[]> callback)
      throws PacketBrokerException {
    try {
      if (observedTopics.add(topic)) {
        pubSubConnection.addListener(new PacketDelegate(callback));
        pubSubConnection.sync().subscribe(topic);
        return true;
      }
      return false;
    } catch (final Exception exception) {
      throw new PacketBrokerException(
          "Could not create observer on channel named %s due to unexpected exception."
              .formatted(topic),
          exception);
    }
  }
}
