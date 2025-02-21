package io.mikeamiry.aegis.lock;

import static com.spotify.futures.CompletableFutures.exceptionallyCompose;
import static io.mikeamiry.aegis.lock.DistributedLockUtils.runLaterAsync;
import static io.mikeamiry.aegis.lock.DistributedLockUtils.supplyLaterAsync;
import static java.util.concurrent.CompletableFuture.failedFuture;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * This class provides mechanisms to execute tasks with distributed locking and retry semantics. It
 * implements an exponential backoff strategy with randomized delay when retries are required. The
 * operations can be performed using either a {@link Supplier} or a {@link Runnable}.
 *
 * <p>The class encapsulates the retry logic based on the maximum number of tries and delays,
 * ensuring a configurable and robust execution mechanism for distributed systems.
 *
 * <p>It uses `CompletableFuture` to provide asynchronous task execution and retry management.
 *
 * <p>Constructor parameters: - `delay`: The initial delay period for retries. - `until`: The upper
 * bound on the total duration for retries or exponential backoff. - `tries`: The maximum number of
 * retry attempts before failing.
 *
 * <p>Methods: - `supply`: Executes a task that returns a result, retrying on failure with
 * exponential backoff. - `execute`: Executes a task that does not return a result, retrying on
 * failure with exponential backoff.
 *
 * <p>Private methods: - `supply`: Internal implementation of `supply`, handling retries and backoff
 * delay calculations. - `execute`: Internal implementation of `execute`, handling retries and
 * backoff delay calculations. - `calculateBackoffDelay`: Computes the backoff delay for retries
 * based on the retry count and configured limits.
 *
 * <p>The retry mechanism throws a {@link RetryingException} if the maximum number of retries is
 * exceeded.
 */
record DistributedLockExecutor(Duration delay, Duration until, int tries) {

  <T> CompletableFuture<T> supply(final Supplier<T> supplier) {
    return supply(supplier, 0, Duration.ZERO);
  }

  private <T> CompletableFuture<T> supply(
      final Supplier<T> supplier, final int retryCount, final Duration backoffDelay) {
    if (retryCount >= tries) {
      return failedFuture(new RetryingException(retryCount));
    }

    return exceptionallyCompose(
            supplyLaterAsync(supplier, calculateBackoffDelay(retryCount + 1)),
            cause -> {
              if (cause instanceof DistributedLockException) {
                return supply(
                    supplier,
                    retryCount + 1,
                    backoffDelay.plus(calculateBackoffDelay(retryCount + 1)));
              }
              return failedFuture(cause);
            })
        .toCompletableFuture();
  }

  CompletableFuture<Void> execute(final Runnable task) {
    return execute(task, 0, Duration.ZERO);
  }

  private CompletableFuture<Void> execute(
      final Runnable action, final int retryCount, final Duration backoffDelay) {
    if (retryCount >= tries) {
      return failedFuture(new RetryingException(retryCount));
    }

    return exceptionallyCompose(
            runLaterAsync(action, calculateBackoffDelay(retryCount + 1)),
            cause -> {
              if (cause instanceof DistributedLockException) {
                return execute(
                    action,
                    retryCount + 1,
                    backoffDelay.plus(calculateBackoffDelay(retryCount + 1)));
              }
              return failedFuture(cause);
            })
        .toCompletableFuture();
  }

  private Duration calculateBackoffDelay(final int retryCount) {
    final long exponentialDelayMillis =
        Math.min(delay.toMillis() * (1L << retryCount), until.toMillis());
    final long randomPart =
        exponentialDelayMillis / 2
            + ThreadLocalRandom.current().nextInt((int) (exponentialDelayMillis / 2));
    return Duration.ofMillis(randomPart);
  }
}
