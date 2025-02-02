package dev.horizon.cache;

import dev.horizon.Horizon;
import dev.horizon.codec.HorizonCodec;
import io.lettuce.core.api.StatefulRedisConnection;
import java.util.function.Supplier;

public class HorizonCache {

  private final String key;
  private final HorizonCodec codec;
  private final StatefulRedisConnection<String, byte[]> connection;

  public HorizonCache(final String key, final Horizon horizon) {
    this.key = key;
    this.codec = horizon.codec();
    this.connection = horizon.connection();
  }

  public <T> void set(final String field, final T value) throws HorizonCacheException {
    performSafely(
        () -> connection.sync().hset(key, field, codec.encode(value)),
        () -> "Failed to put %s at %s in redis-cache".formatted(value, field));
  }

  public <T> T get(final String field, final Class<T> type) throws HorizonCacheException {
    return performSafely(
        () -> codec.decode(connection.sync().hget(key, field), type),
        () -> "Failed to delete %s from redis-cache".formatted(field));
  }

  public boolean remove(final String field) throws HorizonCacheException {
    return performSafely(
        () -> connection.sync().hdel(key, field) > 0,
        () -> "Failed to delete %s from redis-cache".formatted(field));
  }

  public Long clear() throws HorizonCacheException {
    return performSafely(
        () -> connection.sync().del(key),
        () -> "Failed to clear %s from redis-cache".formatted(key));
  }

  private <T, E extends Exception> T performSafely(
      final ThrowingSupplier<T, E> action, final Supplier<String> message)
      throws HorizonCacheException {
    try {
      return action.get();
    } catch (final Exception exception) {
      throw new HorizonCacheException(message.get(), exception);
    }
  }

  @FunctionalInterface
  public interface ThrowingSupplier<T, E extends Throwable> {
    T get() throws E;
  }
}
