package dev.araucaris.horizon.distributed;

public final class RetryingException extends RuntimeException {

  private final int retryCount;

  RetryingException(int retryCount) {
    super("Retried %d times".formatted(retryCount));
    this.retryCount = retryCount;
  }

  public int getRetryCount() {
    return retryCount;
  }
}
