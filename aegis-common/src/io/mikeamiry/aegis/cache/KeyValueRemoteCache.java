package io.mikeamiry.aegis.cache;

import io.lettuce.core.api.StatefulRedisConnection;
import io.mikeamiry.aegis.Aegis;

/**
 * KeyValueRemoteCache is a concrete implementation of the {@link KeyValueCache} interface, designed
 * to provide remote key-value storage functionality using Redis. It supports basic operations like
 * setting, retrieving, and deleting fields within a Redis hash identified by a specific key.
 *
 * <p>This class interacts with a Redis server through a {@link StatefulRedisConnection}, which is
 * obtained from an instance of {@link Aegis}. The key representing the hash in Redis is defined at
 * the time of construction and cannot be modified thereafter.
 *
 * <p>Features and behavior: - Stores key-value pairs within a hash structure in Redis. - Ensures
 * each operation checks for null values for fields and values to avoid exceptions. - Provides
 * thread-safe operations to interact with the underlying Redis server.
 *
 * <p>Typical usage of this class is to support scenarios where a distributed and persistent
 * key-value storage is needed, leveraging Redis capabilities.
 */
public final class KeyValueRemoteCache implements KeyValueCache {

  private final String key;
  private final StatefulRedisConnection<String, String> connection;

  KeyValueRemoteCache(final String key, final Aegis aegis) {
    this.key = key;
    this.connection = aegis.connection();
  }

  @Override
  public boolean set(final String field, final String value) {
    if (field == null || value == null) {
      return false;
    }

    connection.sync().hset(key, field, value);
    return true;
  }

  @Override
  public String get(final String field) {
    if (field == null) {
      return null;
    }

    return connection.sync().hget(key, field);
  }

  @Override
  public boolean del(final String field) {
    if (field == null) {
      return false;
    }

    connection.sync().hdel(key, field);
    return true;
  }
}
