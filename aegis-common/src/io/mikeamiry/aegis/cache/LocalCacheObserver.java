package io.mikeamiry.aegis.cache;

import io.mikeamiry.aegis.Aegis;
import io.mikeamiry.aegis.eventbus.Subscribe;
import io.mikeamiry.aegis.eventbus.Subscriber;

public final class LocalCacheObserver implements Subscriber {

  private final Aegis aegis;

  private LocalCacheObserver(final Aegis aegis) {
    this.aegis = aegis;
  }

  public static LocalCacheObserver of(final Aegis aegis) {
    return new LocalCacheObserver(aegis);
  }

  @Override
  public String topic() {
    return "aegis-caches";
  }

  @Override
  public boolean observer() {
    return true;
  }

  @Subscribe
  public void onInvalidate(final CacheInvalidate invalidate) {
    if (invalidate.identity().equals(aegis.identity())) {
      System.out.println("ignoring local cache invalidate");
      return;
    }

    final KeyValueCache cache = aegis.caches().get(invalidate.key());
    if (cache instanceof final KeyValueLocalCache keyValueLocalCache) {
      keyValueLocalCache.invalidateLocally(invalidate.field());
    }
  }
}
