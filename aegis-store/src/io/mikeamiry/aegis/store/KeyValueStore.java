package io.mikeamiry.aegis.store;

import io.lettuce.core.api.StatefulRedisConnection;
import java.time.Duration;
import java.time.Instant;

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
