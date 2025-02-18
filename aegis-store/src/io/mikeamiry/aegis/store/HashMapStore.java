package io.mikeamiry.aegis.store;

import io.lettuce.core.api.StatefulRedisConnection;
import java.util.Collection;

/**
 * HashMapStore provides an interface for managing hash maps as stored in a Redis database. This
 * interface allows interaction with hash maps represented by a specific key, enabling operations
 * such as setting, retrieving, and deleting fields and values.
 *
 * <p>The interface is implemented by the {@code HashMapStoreImpl} class, ensuring that the
 * underlying operations leverage a Redis connection.
 *
 * <p>The interface supports the following primary operations:
 *
 * <p>1. Setting a value for a specific field within the hash map. 2. Retrieving the value of a
 * specific field within the hash map. 3. Deleting a specific field within the hash map.
 *
 * <p>This interface is designed as a sealed type, restricting its implementation to a specific
 * class.
 */
public sealed interface HashMapStore permits HashMapStoreImpl {

  static HashMapStore create(
      final String key, final StatefulRedisConnection<String, String> connection) {
    return new HashMapStoreImpl(key, connection);
  }

  boolean set(String field, String value);

  String get(String field);

  boolean del(String field);

  Collection<String> values();
}
