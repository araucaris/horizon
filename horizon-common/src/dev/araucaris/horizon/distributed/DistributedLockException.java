package dev.araucaris.horizon.distributed;

public final class DistributedLockException extends RuntimeException {

  DistributedLockException(String message, Throwable cause) {
    super(message, cause);
  }
}
