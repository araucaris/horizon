package io.mikeamiry.aegis.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.lettuce.core.api.StatefulRedisConnection;
import io.mikeamiry.aegis.Aegis;

/**
 * KeyValueLocalCache provides a local caching implementation for a key-value storage system that
 * adheres to the KeyValueCache interface. It utilizes Caffeine as an in-memory cache and
 * synchronizes data with a Redis-backed storage system.
 *
 * <p>This class supports two types of constructors: one that includes a configurable cache size and
 * another with no specific size restrictions for the cache. It integrates with the Aegis system for
 * managing Redis connections and for publishing cache invalidation events.
 *
 * <p>Key Features: - Caches key-value pairs locally for faster retrieval. - Synchronizes data with
 * a Redis database to ensure consistency. - Publishes cache invalidation events through Aegis to
 * ensure distributed coherence. - Provides the ability to invalidate specific cache entries
 * locally.
 *
 * <p>Thread Safety: While the local cache itself is thread-safe as provided by Caffeine, care
 * should be taken when using shared Redis connections. Interactions with Aegis should be handled in
 * a thread-safe manner.
 */
public final class KeyValueLocalCache implements KeyValueCache {

  private final String key;
  private final Aegis aegis;
  private final Cache<String, String> cache;
  private final StatefulRedisConnection<String, String> connection;

  KeyValueLocalCache(final String key, final int cacheSize, final Aegis aegis) {
    this.key = key;
    this.aegis = aegis;
    this.connection = aegis.connection();
    this.cache = Caffeine.newBuilder().maximumSize(cacheSize).build();
  }

  KeyValueLocalCache(final String key, final Aegis aegis) {
    this.key = key;
    this.aegis = aegis;
    this.connection = aegis.connection();
    this.cache = Caffeine.newBuilder().build();
  }

  @Override
  public boolean set(final String field, final String value) {
    if (field == null || value == null) {
      return false;
    }

    cache.put(field, value);
    connection.sync().hset(key, field, value);

    aegis.publish("aegis-caches", new CacheInvalidate(aegis.identity(), key, field));
    return true;
  }

  @Override
  public String get(final String field) {
    if (field == null) {
      return null;
    }

    final String cachedValue = cache.getIfPresent(field);
    if (cachedValue != null) {
      return cachedValue;
    }

    final String remoteValue = connection.sync().hget(key, field);
    if (remoteValue == null) {
      return null;
    }

    cache.put(field, remoteValue);
    return remoteValue;
  }

  @Override
  public boolean del(final String field) {
    if (field == null) {
      return false;
    }

    cache.invalidate(field);
    connection.sync().hdel(key, field);
    aegis.publish("aegis-caches", new CacheInvalidate(aegis.identity(), key, field));
    return true;
  }

  public void invalidateLocally(final String field) {
    cache.invalidate(field);
    System.out.printf("Invalidated %s (%s) %n", field, cache.asMap());
  }
}
