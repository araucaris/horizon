package io.mikeamiry.aegis.store;

import io.lettuce.core.SetArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import java.time.Duration;
import java.time.Instant;

final class KeyValueStoreImpl implements KeyValueStore {

  private final StatefulRedisConnection<String, String> connection;

  KeyValueStoreImpl(final StatefulRedisConnection<String, String> connection) {
    this.connection = connection;
  }

  @Override
  public boolean set(final String key, final String value) {
    return "OK".equals(connection.sync().set(key, value));
  }

  @Override
  public boolean set(final String key, final String value, final Duration ttl) {
    final SetArgs setArgs = new SetArgs();
    final long ttlToMillis = ttl.toMillis();
    if (ttlToMillis > 0) setArgs.px(ttlToMillis);
    return "OK".equals(connection.sync().set(key, value, setArgs));
  }

  @Override
  public boolean set(
      final String key, final String value, final Duration ttl, final boolean onlyIfNotExists) {
    final SetArgs setArgs = new SetArgs();

    final long ttlToMillis = ttl.toMillis();
    if (ttlToMillis > 0) {
      setArgs.px(ttlToMillis);
    }

    if (onlyIfNotExists) {
      setArgs.nx();
    }
    return "OK".equals(connection.sync().set(key, value, setArgs));
  }

  @Override
  public String get(final String key) {
    return connection.sync().get(key);
  }

  @Override
  public boolean del(final String key) {
    return connection.sync().del(key) > 0;
  }

  @Override
  public boolean ttl(final String key, final Instant expireAt) {
    return connection.sync().expireat(key, expireAt);
  }

  @Override
  public long ttl(final String key) {
    return connection.sync().ttl(key);
  }

  @Override
  public long increment(final String key, final long amount) {
    return connection.sync().incrby(key, amount);
  }

  @Override
  public long decrement(final String key, final long amount) {
    return connection.sync().decrby(key, amount);
  }

  @Override
  public boolean contains(final String key) {
    return connection.sync().exists(key) > 0;
  }
}
