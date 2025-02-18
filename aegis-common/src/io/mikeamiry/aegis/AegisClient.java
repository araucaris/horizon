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
 * The AegisClient class provides an implementation of the Aegis interface, enabling distributed
 * caching, locking, and message-broker-based communication. This class is designed to manage
 * Redis-based operations in conjunction with a packet broker for inter-component communication.
 *
 * <p>This class is final and cannot be subclassed. It encapsulates the functionality of multiple
 * components, including Redis connections, local and remote caches, distributed locks, and an
 * observer mechanism. It integrates with a PacketBroker to facilitate a publish-subscribe model and
 * request-response communication.
 *
 * <p>Responsibilities: - Offers APIs to request responses through a channel using the PacketBroker.
 * - Allows for the publishing of packets to specific channels. - Facilitates observation of packets
 * through subscribers. - Maintains local and remote caching mechanisms for data management. -
 * Supports distributed locking mechanisms using Redis-backed locks. - Provides access to Redis
 * connections and the underlying packet broker for advanced usage.
 *
 * <p>Key Features: - Maintains a unique identity for the client instance, typically based on the
 * current process ID. - Supports both local and remote key-value caching with optional size
 * configuration for local caches. - Ensures thread-safety by using concurrent collections for
 * managing caches and locks. - Cleanly shuts down Redis connections and resources upon closure.
 *
 * <p>Methods: - `request` facilitates sending a packet on a channel and returns a CompletableFuture
 * for handling responses. - `observe` allows registering a subscriber to observe incoming messages
 * on specific channels. - `publish` publishes a packet to a specific channel. - `getRemoteCache`,
 * `getLocalCache` provide methods to retrieve or create caches. - `getLock` provides a mechanism to
 * create or retrieve distributed locks with retry configuration. - Accessor methods return the
 * identity, Redis client, caches, locks, and Redis connections.
 *
 * <p>Thread Safety: - Caches and locks are maintained using ConcurrentHashMap to ensure thread-safe
 * access and modification.
 *
 * <p>Closing the Client: - Implements the `Closeable` interface for properly shutting down
 * resources such as Redis connections. - On closure, it ensures Redis client and connections are
 * terminated, throwing an exception if resource closure fails.
 */
final class AegisClient implements Closeable, Aegis {

  private final String identity;

  private final RedisClient redisClient;
  private final PacketBroker packetBroker;

  private final StatefulRedisConnection<String, String> connection;
  private final StatefulRedisPubSubConnection<String, String> pubSubConnection;

  AegisClient(
      final String identity, final RedisClient redisClient, final PacketBroker packetBroker) {
    this.identity = identity;
    this.redisClient = redisClient;
    this.connection = redisClient.connect();
    this.pubSubConnection = redisClient.connectPubSub();
    this.packetBroker = packetBroker;
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
    return KeyValueStore.create(connection);
  }

  @Override
  public HashMapStore map(final String name) {
    return HashMapStore.create(name, connection);
  }

  @Override
  public DistributedLock getLock(final String key, final int tries) {
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
