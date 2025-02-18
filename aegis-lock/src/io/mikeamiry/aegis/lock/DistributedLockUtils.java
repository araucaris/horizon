package io.mikeamiry.aegis.lock;

import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.concurrent.ForkJoinPool.commonPool;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.function.Supplier;

/**
 * Utility class for managing delayed execution of tasks using asynchronous mechanisms and a shared
 * scheduler.
 *
 * <p>This class provides static methods to execute tasks after a specified delay, either as
 * asynchronous computations returning results or as non-returning runnable actions. It also
 * supports creating custom executors that execute tasks after a delay.
 *
 * <p>Key features of this utility include:
 *
 * <p>- Facilitates delayed execution of tasks with both {@link
 * CompletableFuture#runAsync(Runnable)} and {@link CompletableFuture#supplyAsync(Supplier)}
 * mechanisms. - Provides creation of delayed {@link Executor}, either with defaults or with a
 * custom backing executor. - Leverages a {@link ScheduledExecutorService} for scheduling delayed
 * executions in a thread-safe manner.
 *
 * <p>This class is designed to support scenarios where delayed execution is a core requirement,
 * such as retry mechanisms, deferred lock acquisition, or periodic task scheduling. It is
 * thread-safe and meant for high concurrency environments.
 *
 * <p>This class is immutable and not meant to be extended.
 */
final class DistributedLockUtils {

  @SuppressWarnings("ScheduledThreadPoolExecutorWithZeroCoreThreads")
  static final ScheduledExecutorService SCHEDULER = new ScheduledThreadPoolExecutor(0);

  private DistributedLockUtils() {}

  static CompletableFuture<Void> runLaterAsync(final Runnable runnable, final Duration delay) {
    return runAsync(runnable, delayedExecutor(delay.toMillis()));
  }

  static <T> CompletableFuture<T> supplyLaterAsync(
      final Supplier<T> supplier, final Duration delay) {
    return supplyAsync(supplier, delayedExecutor(delay.toMillis()));
  }

  static Executor delayedExecutor(final long delay) {
    return delayedExecutor(delay, commonPool());
  }

  static Executor delayedExecutor(final long delay, final Executor executor) {
    return task -> SCHEDULER.schedule(() -> executor.execute(task), delay, MILLISECONDS);
  }
}
