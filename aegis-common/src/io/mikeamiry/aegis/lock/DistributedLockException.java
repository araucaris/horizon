package io.mikeamiry.aegis.lock;

/**
 * Exception thrown when a distributed lock operation encounters an error.
 *
 * <p>This exception is typically used to signal issues during distributed lock acquisition or
 * usage. It extends {@link IllegalStateException} and can be used to indicate that a lock cannot be
 * acquired or is currently held by another process in a distributed system.
 *
 * <p>Instances of this exception are immutable.
 */
public final class DistributedLockException extends IllegalStateException {

  DistributedLockException(final String message) {
    super(message);
  }
}
