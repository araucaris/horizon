package dev.horizon.cache;

import dev.horizon.HorizonException;

public final class HorizonCacheException extends HorizonException {

  public HorizonCacheException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public HorizonCacheException(final String message) {
    super(message);
  }
}
