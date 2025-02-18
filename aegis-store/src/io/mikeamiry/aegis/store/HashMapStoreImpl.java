package io.mikeamiry.aegis.store;

import io.lettuce.core.api.StatefulRedisConnection;
import java.util.Collection;

/**
 * Implementation of the {@link HashMapStore} interface for managing hash maps stored in Redis. This
 * class provides methods to interact with hash maps in a Redis database based on a specified key.
 *
 * <p>This implementation uses a {@link StatefulRedisConnection} to execute hash map operations such
 * as: - Setting a field and value in the hash map. - Retrieving the value associated with a
 * specific field. - Deleting a field from the hash map.
 */
final class HashMapStoreImpl implements HashMapStore {

  private final String key;
  private final StatefulRedisConnection<String, String> connection;

  HashMapStoreImpl(final String key, final StatefulRedisConnection<String, String> connection) {
    this.key = key;
    this.connection = connection;
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

  @Override
  public Collection<String> values() {
    return connection.sync().hvals(key);
  }
}
