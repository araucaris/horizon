package dev.horizon.lock;

import dev.horizon.HorizonException;

public final class DistributedLockException extends HorizonException {

  DistributedLockException(final String message, final Throwable cause) {
    super(message, cause);
  }

  DistributedLockException(final String message) {
    super(message);
  }
}
