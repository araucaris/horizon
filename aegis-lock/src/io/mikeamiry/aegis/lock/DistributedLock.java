package io.mikeamiry.aegis.lock;

import static io.mikeamiry.aegis.lock.DistributedLockUtils.SCHEDULER;
import static java.time.Duration.ofMillis;
import static java.time.Duration.ofSeconds;
import static java.time.Instant.now;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.logging.Level.SEVERE;

import io.mikeamiry.aegis.store.KeyValueStore;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Represents a distributed lock mechanism that allows safe execution of tasks or operations in a
 * distributed environment. This class ensures only one instance of a process can acquire the lock
 * for a specific key at a time.
 *
 * <p>The lock utilizes a key-value store to manage ownership and duration of the lock. It supports
 * operations on tasks with the help of asynchronous execution using {@link
 * DistributedLockExecutor}.
 *
 * <p>The lock is identified by a unique key (`lockKey`) and an identity string (`identity`)
 * representing the owner. The lock manages its expiration (`until`) and allows retry attempts with
 * configurable delay and maximum tries. The class provides automatic TTL (time-to-live) updates
 * while the lock is being held.
 *
 * <p>Features: - Lock acquisition and enforcement using a key-value store. - Automatic and periodic
 * TTL updates for the lock. - Asynchronous and retry-enabled task execution support.
 *
 * <p>Private Constructor: The lock instance is created using the static `create` methods.
 *
 * <p>Thread-Safety: This class is designed to be thread-safe when used in a distributed context
 * with proper configuration of the backing key-value store.
 *
 * <p>Methods: - static `create(String, String, Duration, Duration, int, KeyValueStore)`: Creates a
 * DistributedLock with specific configurations. - static `create(String, String, int,
 * KeyValueStore)`: Creates a DistributedLock with default delay and expiration durations. -
 * `CompletableFuture supply(Supplier)`: Executes a task that produces a result, ensuring locking
 * semantics. - `CompletableFuture execute(Runnable)`: Executes a task without a result, ensuring
 * locking semantics.
 *
 * <p>Exceptions: - Throws {@link DistributedLockException} if the lock is already held by another
 * identity during execution attempts. - Relies on the underlying key-value store for
 * storage-related exceptions.
 */
public final class DistributedLock {

  private static final Logger log = Logger.getLogger(DistributedLock.class.getName());

  private final String lockKey;
  private final String identity;
  private final DistributedLockExecutor executor;
  private final Duration until;
  private final KeyValueStore store;
  private ScheduledFuture<?> future;

  private DistributedLock(
      final String lockKey,
      final String identity,
      final Duration delay,
      final Duration until,
      final int tries,
      final KeyValueStore store) {
    this.lockKey = lockKey;
    this.identity = identity;
    this.until = until;
    this.store = store;
    this.executor = new DistributedLockExecutor(delay, until, tries);
  }

  public static DistributedLock create(
      final String lockKey,
      final String identity,
      final Duration delay,
      final Duration until,
      final int tries,
      final KeyValueStore store) {
    return new DistributedLock(lockKey, identity, delay, until, tries, store);
  }

  public static DistributedLock create(
      final String lockKey, final String identity, final int tries, final KeyValueStore store) {
    return create(lockKey, identity, ofMillis(150L), ofSeconds(3L), tries, store);
  }

  public <T> CompletableFuture<T> supply(final Supplier<T> supplier) {
    return executor.supply(
        () -> {
          final String ownerIdentity = store.get(lockKey);
          if (ownerIdentity == null) {
            final boolean acquired = store.set(lockKey, identity, until, true);
            if (!acquired) {
              throw new DistributedLockException("Lock is already held by another process.");
            }

            try {
              startWatching();
              return supplier.get();
            } finally {
              stopWatching();
              store.del(lockKey);
            }
          }

          throw new DistributedLockException(
              "Lock is currently held by %s".formatted(ownerIdentity));
        });
  }

  public CompletableFuture<Void> execute(final Runnable task) {
    return executor.execute(
        () -> {
          final String ownerIdentity = store.get(lockKey);
          if (ownerIdentity == null) {
            final boolean acquired = store.set(lockKey, identity, until, true);
            if (!acquired) {
              throw new DistributedLockException("Lock is already held by another process.");
            }

            try {
              startWatching();
              task.run();
            } finally {
              stopWatching();
              store.del(lockKey);
            }
            return;
          }

          throw new DistributedLockException(
              "Lock is currently held by %s".formatted(ownerIdentity));
        });
  }

  private void startWatching() {
    future =
        SCHEDULER.scheduleAtFixedRate(
            () -> {
              try {
                store.ttl(lockKey, now().plus(until));
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
}
