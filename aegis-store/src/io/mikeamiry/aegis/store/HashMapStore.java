package io.mikeamiry.aegis.store;

import io.lettuce.core.api.StatefulRedisConnection;

public sealed interface HashMapStore permits HashMapStoreImpl {

  static HashMapStore create(
      final String key, final StatefulRedisConnection<String, String> connection) {
    return new HashMapStoreImpl(key, connection);
  }

  boolean set(String field, String value);

  String get(String field);

  boolean del(String field);
}
