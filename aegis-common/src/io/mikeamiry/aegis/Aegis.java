package io.mikeamiry.aegis;

import io.lettuce.core.RedisClient;
import io.mikeamiry.aegis.broker.Packet;
import io.mikeamiry.aegis.broker.PacketBrokerException;
import io.mikeamiry.aegis.eventbus.Observer;
import io.mikeamiry.aegis.lock.DistributedLock;
import io.mikeamiry.aegis.store.HashMapStore;
import io.mikeamiry.aegis.store.KeyValueStore;
import java.io.Closeable;
import java.util.concurrent.CompletableFuture;

/**
 * Aegis is a sealed interface representing a comprehensive system for managing packet
 * communication, key-value storage, distributed locks, and observer subscriptions. It extends
 * {@link Closeable}, allowing implementations to release resources when the system is no longer in
 * use.
 *
 * <p>Key functionalities provided by Aegis:
 *
 * <p>- **Request-Response Packet Communication**: Facilitates sending requests and receiving
 * responses over specified channels using {@code request}.
 *
 * <p>- **Observer Subscriptions**: Allows observers to subscribe and monitor events or packets.
 *
 * <p>- **Packet Publishing**: Enables publishing packets to specified channels.
 *
 * <p>- **Key-Value Store Management**: Provides access to a {@link KeyValueStore} for managing
 * key-value pairs.
 *
 * <p>- **HashMap Store Feature**: Handles hash-based storage using a {@link HashMapStore},
 * represented by a specific key name.
 *
 * <p>- **Distributed Locking**: Supports distributed locking mechanisms with retry capabilities
 * using {@code lock }.
 *
 * <p>- **Identity Resolution**: Retrieves the unique identity of the underlying Aegis instance.
 *
 * <p>- **Redis Client Access**: Provides access to the underlying {@link RedisClient} for extended
 * operations.
 *
 * <p>Methods: - {@code <T extends Packet> CompletableFuture<T> request(String channel, Packet
 * request)}: Sends a request packet to a specified channel and returns a future that resolves with
 * the response.
 *
 * <p>- {@code void observe(Observer observer) throws PacketBrokerException}: Subscribes an observer
 * to listen to events or packets, throwing {@link PacketBrokerException} on failure.
 *
 * <p>- {@code void publish(String channel, Packet packet) throws PacketBrokerException}: Publishes
 * a packet to a channel, with potential {@link PacketBrokerException} for publishing errors.
 *
 * <p>- {@code KeyValueStore kv()}: Provides access to a key-value store implementation.
 *
 * <p>- {@code HashMapStore map(String name)}: Provides access to a hash map storage interface
 * identified by a name.
 *
 * <p>- {@code DistributedLock lock (String key, int tries)}: Returns a distributed lock object for
 * synchronizing processes, with retry attempts specified.
 *
 * <p>- {@code String identity()}: Retrieves the system's identifier.
 *
 * <p>- {@code RedisClient redisClient()}: Returns the underlying {@link RedisClient} for additional
 * Redis operations.
 *
 * <p>Subclasses must implement the permitted sealed interface {@link AegisClient}.
 */
public sealed interface Aegis extends Closeable permits AegisClient {

  <T extends Packet> CompletableFuture<T> request(String channel, Packet request);

  void observe(Observer observer) throws PacketBrokerException;

  void publish(String channel, Packet packet) throws PacketBrokerException;

  KeyValueStore kv();

  HashMapStore map(final String name);

  DistributedLock lock(String key, int tries);

  String identity();

  RedisClient redisClient();
}
