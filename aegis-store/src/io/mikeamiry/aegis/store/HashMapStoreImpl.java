package io.mikeamiry.aegis.store;

import io.lettuce.core.api.StatefulRedisConnection;

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
}
