package io.mikeamiry.aegis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.mikeamiry.aegis.cache.KeyValueCache;
import io.mikeamiry.aegis.eventbus.Observer;
import io.mikeamiry.aegis.lock.DistributedLock;
import io.mikeamiry.aegis.packet.Packet;
import io.mikeamiry.aegis.packet.PacketBrokerException;
import java.io.Closeable;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Aegis is a sealed interface that acts as the core abstraction for distributed systems operations,
 * including caching, locking, messaging, and observation functionalities. It provides methods to
 * facilitate communication and resource management in a distributed environment.
 *
 * <p>This interface is restricted to being implemented by the AegisClient class, ensuring
 * controlled extensibility. It extends the {@link Closeable} interface, requiring implementations
 * to manage resources and support proper cleanup of connections and other resources.
 *
 * <p>Key capabilities of Aegis include:
 *
 * <p>- **Request/Response Mechanism**: Enables asynchronous communication using serialized {@link
 * Packet} objects. - **Observation**: Facilitates subscribing to topics for real-time updates using
 * a {@link Observer}. - **Publishing Messages**: Allows the publication of messages to specific
 * channels using {@link Packet} objects. - **Key-Value Caching**: Provides remote and locally
 * cached storage mechanisms through the {@link KeyValueCache} interface. - **Distributed Locks**:
 * Offers distributed locking mechanisms using keys to ensure concurrency control via {@link
 * DistributedLock}. - **Identity Access**: Exposes the identity of the Aegis instance for
 * cluster-wide identification purposes. - **Redis Integrations**: Provides access to underlying
 * Redis resources using {@link RedisClient} and related connections.
 *
 * <p>Methods included in this interface cover a range of functionalities:
 *
 * <p>- `request(String, Packet)`: Sends a request to a channel and asynchronously waits for a
 * response. - `observe(Observer)`: Registers a {@link Observer} to observe packets or events on
 * relevant topics. - `publish(String, Packet)`: Publishes a {@link Packet} to a channel for
 * consumption by observers. - `getRemoteCache(String)`, `getLocalCache(String)`,
 * `getLocalCache(String, int)`: Retrieve or create caches for efficient data storage. -
 * `getLock(String, int)`: Acquires a distributed lock for key-based synchronization. -
 * `identity()`: Retrieves the unique identifier of the Aegis instance. - `redisClient()`: Provides
 * access to the underlying {@link RedisClient} object. - `caches()`: Returns a map of all currently
 * managed caches. - `locks()`: Returns a map of all currently managed distributed locks. -
 * `connection()`: Gives access to the main Redis connection. - `pubSubConnection()`: Provides
 * access to the Redis publish-subscribe connection. - `close()`: Closes the distributed system
 * resources and cleans up any active Redis connections.
 */
public sealed interface Aegis extends Closeable permits AegisClient {

  <T extends Packet> CompletableFuture<T> request(String channel, Packet request);

  void observe(Observer observer) throws PacketBrokerException;

  void publish(String channel, Packet packet) throws PacketBrokerException;

  KeyValueCache getRemoteCache(String key);

  KeyValueCache getLocalCache(String key);

  KeyValueCache getLocalCache(String key, int cacheSize);

  DistributedLock getLock(String key, int tries);

  String identity();

  RedisClient redisClient();

  Map<String, KeyValueCache> caches();

  Map<String, DistributedLock> locks();

  StatefulRedisConnection<String, String> connection();

  StatefulRedisPubSubConnection<String, String> pubSubConnection();
}
