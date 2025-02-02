package dev.horizon.lock;

public final class RetryingException extends RuntimeException {

  private final int retryCount;

  RetryingException(final int retryCount) {
    super("Retried %d times".formatted(retryCount));
    this.retryCount = retryCount;
  }

  public int retryCount() {
    return retryCount;
  }
}
