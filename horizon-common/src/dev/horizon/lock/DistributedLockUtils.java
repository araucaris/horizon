package dev.horizon.lock;

import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.concurrent.ForkJoinPool.commonPool;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

final class DistributedLockUtils {
  private static final ScheduledExecutorService SCHEDULER = new ScheduledThreadPoolExecutor(0);

  private DistributedLockUtils() {}

  static CompletableFuture<Void> runLater(final Runnable runnable, final Duration delay) {
    return runAsync(
        runnable,
        task ->
            SCHEDULER.schedule(() -> commonPool().execute(task), delay.toMillis(), MILLISECONDS));
  }
}
