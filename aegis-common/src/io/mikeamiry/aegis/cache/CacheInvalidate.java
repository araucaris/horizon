package io.mikeamiry.aegis.cache;

import io.mikeamiry.aegis.packet.Packet;

/**
 * Represents a cache invalidation event triggered when a specific cache entry is invalidated. This
 * class is used to communicate the invalidation details, including the identity of the source, the
 * cache key, and the field within the cache.
 *
 * <p>Instances of this class encapsulate the information required to identify which cache entry
 * should be invalidated and are typically published to a messaging system for downstream consumers
 * to act upon.
 *
 * <p>CacheInvalidate is immutable once created and provides getter methods to retrieve the details
 * of the invalidation event.
 *
 * <p>Inherits from the Packet class to align with the broader communication and event model in
 * which it operates.
 */
public class CacheInvalidate extends Packet {

  private String identity;
  private String key;
  private String field;

  private CacheInvalidate() {}

  public CacheInvalidate(final String identity, final String key, final String field) {
    this.identity = identity;
    this.key = key;
    this.field = field;
  }

  public String identity() {
    return identity;
  }

  public String key() {
    return key;
  }

  public String field() {
    return field;
  }
}
