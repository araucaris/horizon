package io.mikeamiry.aegis.store;

import io.lettuce.core.api.StatefulRedisConnection;
import java.time.Duration;
import java.time.Instant;

/**
 * Represents a key-value store interface for managing key-value pairs.
 *
 * <p>This interface defines operations commonly used in a key-value store system. It is sealed, and
 * the permitted implementation is KeyValueStoreImpl. It is designed to allow applications to
 * interact with key-value pairs stored in a Redis database. The interface provides both basic
 * operations and advanced features.
 *
 * <p>The primary functionalities include: - Setting key-value pairs, optionally with time-to-live
 * (TTL) and conditional options. - Retrieving the value of a specific key from the store. -
 * Deleting a key and its associated value. - Managing expiration times for keys with the ability to
 * set future expiry dates. - Performing atomic operations such as incrementing and decrementing
 * long numeric values. - Determining whether a key exists in the store.
 */
public sealed interface KeyValueStore permits KeyValueStoreImpl {

  static KeyValueStore create(final StatefulRedisConnection<String, String> connection) {
    return new KeyValueStoreImpl(connection);
  }

  boolean set(String key, String value);

  boolean set(String key, String value, Duration ttl);

  boolean set(String key, String value, Duration ttl, boolean onlyIfNotExists);

  String get(String key);

  boolean del(String key);

  boolean ttl(String key, Instant expireAt);

  long ttl(String key);

  long increment(String key, long amount);

  long decrement(String key, long amount);

  boolean contains(String key);
}
