package io.mikeamiry.aegis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.mikeamiry.aegis.broker.Packet;
import io.mikeamiry.aegis.broker.PacketBroker;
import io.mikeamiry.aegis.broker.PacketBrokerException;
import io.mikeamiry.aegis.eventbus.Observer;
import io.mikeamiry.aegis.lock.DistributedLock;
import io.mikeamiry.aegis.store.HashMapStore;
import io.mikeamiry.aegis.store.KeyValueStore;
import java.io.Closeable;
import java.util.concurrent.CompletableFuture;

/**
 * AegisClient is a final implementation of the {@link Aegis} interface, providing a comprehensive
 * system for managing packet communication, key-value storage, distributed locks, observer
 * subscriptions, and Redis client interactions. This class encapsulates all core operations and
 * ensures proper resource management through the {@code Closeable} interface.
 *
 * <p>Key features:
 *
 * <p>- Facilitates asynchronous request-response packet communication using the {@code request}
 * method. - Allows observing events and subscribing observers via the {@code observe} method. -
 * Publishes packets to specified channels using the {@code publish} method. - Provides access to a
 * key-value store using the {@code kv} method. - Enables hash-based data storage through the {@code
 * map} method. - Supports distributed locking mechanisms using the {@code lock} method. - Retrieves
 * the system's unique identity with the {@code identity} method. - Offers access to the underlying
 * Redis client through the {@code redisClient} method.
 *
 * <p>This implementation relies on {@link RedisClient} for managing Redis connections and a {@link
 * PacketBroker} for handling packet-based communication. It ensures resource cleanup by closing
 * Redis connections and shutting down the Redis client during the {@code close} method invocation.
 *
 * <p>Key methods:
 *
 * <p>- {@code <T extends Packet> CompletableFuture<T> request(String channel, Packet request)}:
 * Sends a request packet and asynchronously retrieves the response.
 *
 * <p>- {@code void observe(Observer observer) throws PacketBrokerException}: Registers an observer
 * for event or packet subscriptions.
 *
 * <p>- {@code void publish(String channel, Packet packet) throws PacketBrokerException}: Publishes
 * a packet to a channel.
 *
 * <p>- {@code KeyValueStore kv()}: Provides access to a key-value store for managing key-value
 * pairs.
 *
 * <p>- {@code HashMapStore map(String name)}: Returns a hash map store associated with a given
 * name.
 *
 * <p>- {@code DistributedLock lock(String key, int tries)}: Creates a distributed lock for
 * synchronization with retry handling.
 *
 * <p>- {@code String identity()}: Retrieves the unique identity of this Aegis client.
 *
 * <p>- {@code RedisClient redisClient()}: Returns the underlying Redis client instance.
 *
 * <p>Resource management is a critical aspect of this class. The {@code close()} method ensures
 * that all Redis connections are terminated and the {@link PacketBroker} operations are safely
 * concluded. Any exceptions during resource cleanup will result in an {@link AegisException} being
 * thrown.
 */
final class AegisClient implements Closeable, Aegis {

  private final String identity;

  private final RedisClient redisClient;
  private final PacketBroker packetBroker;

  private final KeyValueStore keyValueStore;

  private final StatefulRedisConnection<String, String> connection;
  private final StatefulRedisPubSubConnection<String, String> pubSubConnection;

  AegisClient(
      final String identity, final RedisClient redisClient, final PacketBroker packetBroker) {
    this.identity = identity;
    this.redisClient = redisClient;
    this.connection = redisClient.connect();
    this.pubSubConnection = redisClient.connectPubSub();
    this.packetBroker = packetBroker;
    this.keyValueStore = KeyValueStore.create(connection);
  }

  @Override
  public <T extends Packet> CompletableFuture<T> request(
      final String channel, final Packet request) {
    return packetBroker.request(channel, request);
  }

  @Override
  public void observe(final Observer observer) throws PacketBrokerException {
    packetBroker.observe(observer);
  }

  @Override
  public void publish(final String channel, final Packet packet) throws PacketBrokerException {
    packetBroker.publish(channel, packet);
  }

  @Override
  public KeyValueStore kv() {
    return keyValueStore;
  }

  @Override
  public HashMapStore map(final String name) {
    return HashMapStore.create(name, connection);
  }

  @Override
  public DistributedLock lock(final String key, final int tries) {
    return DistributedLock.create(key, identity, tries, kv());
  }

  @Override
  public String identity() {
    return identity;
  }

  @Override
  public RedisClient redisClient() {
    return redisClient;
  }

  @Override
  public void close() {
    try {
      connection.close();
      pubSubConnection.close();
      redisClient.shutdown();
    } catch (final Exception exception) {
      throw new AegisException("Could not close Aegis due to unexpected exception.", exception);
    }
  }
}
