package dev.araucaris.horizon.storage;

import dev.araucaris.horizon.serdes.HorizonSerdes;
import io.lettuce.core.api.StatefulRedisConnection;
import java.util.function.Supplier;

public class HorizonStorage {

  private final String namespace;
  private final HorizonSerdes horizonSerdes;
  private final StatefulRedisConnection<String, byte[]> connection;

  HorizonStorage(
      String namespace,
      HorizonSerdes horizonSerdes,
      StatefulRedisConnection<String, byte[]> connection) {
    this.namespace = namespace;
    this.horizonSerdes = horizonSerdes;
    this.connection = connection;
  }

  public static HorizonStorage create(
      String namespace,
      HorizonSerdes horizonSerdes,
      StatefulRedisConnection<String, byte[]> connection) {
    return new HorizonStorage(namespace, horizonSerdes, connection);
  }

  public <T> boolean set(String key, T value) throws StorageException {
    return performSafely(
        () -> connection.sync().hset(namespace, key, horizonSerdes.encode(value)),
        () -> "Failed to put %s at %s in redis-cache".formatted(value, key));
  }

  public <T> T get(String key, Class<T> type) throws StorageException {
    return performSafely(
        () -> horizonSerdes.decode(connection.sync().hget(namespace, key), type),
        () -> "Failed to delete %s from redis-cache".formatted(key));
  }

  public boolean remove(String key) throws StorageException {
    return performSafely(
        () -> connection.sync().hdel(namespace, key) > 0,
        () -> "Failed to delete %s from redis-cache".formatted(key));
  }

  public Long clear() throws StorageException {
    return performSafely(
        () -> connection.sync().del(namespace),
        () -> "Failed to clear %s from redis-cache".formatted(namespace));
  }

  private <T, E extends Exception> T performSafely(
      ThrowingSupplier<T, E> action, Supplier<String> message) throws StorageException {
    try {
      return action.get();
    } catch (Exception exception) {
      throw new StorageException(message.get(), exception);
    }
  }

  @FunctionalInterface
  public interface ThrowingSupplier<T, E extends Throwable> {
    T get() throws E;
  }
}
