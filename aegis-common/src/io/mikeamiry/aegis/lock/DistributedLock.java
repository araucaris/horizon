package io.mikeamiry.aegis.lock;

import static io.mikeamiry.aegis.lock.DistributedLockUtils.SCHEDULER;
import static java.lang.System.currentTimeMillis;
import static java.time.Duration.ofMillis;
import static java.time.Duration.ofSeconds;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.logging.Level.SEVERE;

import io.mikeamiry.aegis.cache.KeyValueCache;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Provides a mechanism for distributed locking, enabling tasks to be executed while ensuring
 * mutually exclusive access to a shared resource in a distributed system.
 *
 * <p>The {@code DistributedLock} class leverages a {@code KeyValueCache} to manage lock state and
 * ensures lock validity by periodically refreshing it if necessary. The locking mechanism supports
 * retrying operations with back-off in the event that the lock is currently held by another
 * process.
 *
 * <p>Locks are acquired and released automatically ensuring proper lifecycle management and
 * preventing stale locks in case of failure.
 *
 * <p>Methods: - {@link #create(String, String, Duration, Duration, int, KeyValueCache)} creates an
 * instance of DistributedLock with specified parameters for lock acquisition delays, lifetime
 * limits, and retries. - {@link #create(String, String, int, KeyValueCache)} provides a more
 * simplified version of the create method with default delays and durations. - {@link
 * #supply(Supplier)} executes a task that returns a value, wrapped in a CompletableFuture, while
 * adhering to the distributed lock policies. - {@link #execute(Runnable)} executes a non-returning
 * task, adhering similarly to distributed lock policies.
 *
 * <p>The lock lifecycle management: - The {@code startWatching()} method ensures that the lock’s
 * validity is automatically extended periodically while the lock is held. - The {@code
 * stopWatching()} method stops the periodic lock validation once the lock is no longer needed. -
 * The {@code extendLockValidity()} method extends the lock’s expiration time based on the current
 * system time.
 *
 * <p>Underlying utility of DistributedLock depends on: - {@code DistributedLockContext}, which
 * encapsulates the identity of the owner of the lock and the expiration timestamp. - {@code
 * DistributedLockExecutor}, which handles retry logic and exponential back-off for lock acquisition
 * attempts. - {@code KeyValueCache}, for persisting lock states in a distributed key-value store.
 *
 * <p>This class is immutable and final, ensuring that its behavior cannot be extended or altered
 * further.
 */
public final class DistributedLock {

  private static final Logger log = Logger.getLogger(DistributedLock.class.getName());

  private final String lockKey;
  private final DistributedLockContext context;
  private final DistributedLockExecutor executor;
  private final KeyValueCache cache;
  private ScheduledFuture<?> future;

  private DistributedLock(
      final String identity,
      final String lockKey,
      final Duration delay,
      final Duration until,
      final int tries,
      final KeyValueCache cache) {
    this.lockKey = lockKey;
    this.cache = cache;
    this.context = new DistributedLockContext(identity, currentTimeMillis() + until.toMillis());
    this.executor = new DistributedLockExecutor(delay, until, tries);
  }

  public static DistributedLock create(
      final String identity,
      final String lockKey,
      final Duration delay,
      final Duration until,
      final int tries,
      final KeyValueCache cache) {
    return new DistributedLock(identity, lockKey, delay, until, tries, cache);
  }

  public static DistributedLock create(
      final String identity, final String lockKey, final int tries, final KeyValueCache cache) {
    return create(identity, lockKey, ofMillis(150L), ofSeconds(3L), tries, cache);
  }

  public <T> CompletableFuture<T> supply(final Supplier<T> supplier) {
    return executor.supply(
        () -> {
          final String serializedContext = cache.get(lockKey);
          if (serializedContext == null) {
            cache.set(lockKey, context.toString());

            startWatching();
            final T value = supplier.get();
            stopWatching();
            cache.del(lockKey);
            return value;
          }

          final DistributedLockContext parsedContext =
              DistributedLockContext.parse(serializedContext);
          if (parsedContext.expiresAt() < currentTimeMillis()) {
            extendLockValidity();

            startWatching();
            final T value = supplier.get();
            stopWatching();

            cache.del(lockKey);

            return value;
          }

          throw new DistributedLockException(
              "Lock is currently held by %s".formatted(serializedContext));
        });
  }

  public CompletableFuture<Void> execute(final Runnable task) {
    return executor.execute(
        () -> {
          final String lock = cache.get(lockKey);
          if (lock == null) {
            cache.set(lockKey, context.toString());

            startWatching();
            task.run();
            stopWatching();

            cache.del(lockKey);
            return;
          }

          final DistributedLockContext remoteContext = DistributedLockContext.parse(lock);
          if (remoteContext.expiresAt() < currentTimeMillis()) {
            extendLockValidity();

            startWatching();
            task.run();
            stopWatching();

            cache.del(lockKey);
            return;
          }

          throw new DistributedLockException("Lock is currently held by " + lock);
        });
  }

  private void startWatching() {
    future =
        SCHEDULER.scheduleAtFixedRate(
            () -> {
              try {
                if (context.expiresAt() < (currentTimeMillis() + executor.delay().toMillis())) {
                  return;
                }

                extendLockValidity();
              } catch (final Exception exception) {
                log.log(SEVERE, "Error watching lock.", exception);
              }
            },
            1L,
            1L,
            SECONDS);
  }

  private void stopWatching() {
    if (future != null) {
      future.cancel(false);
    }
  }

  private void extendLockValidity() {
    context.expiresAt(currentTimeMillis() + executor.until().toMillis());
    cache.set(lockKey, context.toString());
  }
}
