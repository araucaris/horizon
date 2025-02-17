package io.mikeamiry.aegis.cache;

import io.mikeamiry.aegis.Aegis;

/**
 * KeyValueCache serves as a sealed interface for managing key-value storage systems. It defines
 * common operations for setting, getting, and deleting key-value pairs within a cache, while
 * supporting both local and remote cache implementations.
 *
 * <p>This interface is implemented by the following concrete classes: - {@link
 * KeyValueRemoteCache}: For remotely backed key-value storage using Redis. - {@link
 * KeyValueLocalCache}: For locally cached key-value storage with optional size limitations, backed
 * by Caffeine and synchronized with Redis.
 *
 * <p>Static factory methods are provided to create instances of remote or locally cached
 * implementations with customization options like cache size.
 */
public sealed interface KeyValueCache permits KeyValueRemoteCache, KeyValueLocalCache {

  static KeyValueCache createRemote(final String key, final Aegis aegis) {
    return new KeyValueRemoteCache(key, aegis);
  }

  static KeyValueCache createLocallyCached(final String key, final Aegis aegis) {
    return new KeyValueLocalCache(key, aegis);
  }

  static KeyValueCache createLocallyCached(
      final String key, final int cacheSize, final Aegis aegis) {
    return new KeyValueLocalCache(key, cacheSize, aegis);
  }

  boolean set(String field, String value);

  String get(String field);

  boolean del(String field);
}
