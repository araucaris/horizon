package io.mikeamiry.aegis.lock;

import static io.mikeamiry.aegis.lock.DistributedLockUtils.SCHEDULER;
import static java.time.Duration.ofMillis;
import static java.time.Duration.ofSeconds;
import static java.time.Instant.now;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.logging.Level.SEVERE;

import io.mikeamiry.aegis.store.KeyValueStore;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Provides a mechanism for distributed locking.
 *
 * <p>This class is designed for distributed systems to ensure synchronized access to shared
 * resources by leveraging a key-value store for lock management. The functionality includes
 * acquiring locks, releasing locks, and executing tasks with the protection of a distributed lock.
 *
 * <p>DistributedLock uses a combination of identifiers (key and identity) and expiration time (TTL)
 * to manage locks in the system. Attempted operations ensure atomicity and safety by leveraging the
 * underlying {@link KeyValueStore}.
 *
 * <p>Features include: - Asynchronous execution with distributed lock acquisition. - Task retries
 * with exponential backoff in case of failures. - Automatic renewal of lock TTL while the lock is
 * held.
 *
 * <p>Constructor: The constructor is private, and instances of the DistributedLock are created
 * using provided static `create` factory methods.
 *
 * <p>Key public methods: - `create`: Factory methods to construct a new instance with specified
 * parameters or defaults. - `supply`: Executes a task with a return value under the lock
 * protection. - `execute`: Executes a task without a return value under the lock protection. -
 * `tryExecuteOnce`: Attempts to execute a task once under lock protection, returning whether the
 * lock was successfully acquired.
 *
 * <p>Private internal behaviors include: - A periodic task (`startWatching`) to renew the TTL of
 * the lock while it is held to prevent premature expiration. - Cleanup of resources and state when
 * the lock is released (`stopWatching`).
 *
 * <p>Exceptions: - Throws {@link DistributedLockException} when a lock cannot be acquired,
 * typically indicating that another process currently holds the lock.
 *
 * <p>Thread Safety: - This class is designed to be thread-safe when interacting with a distributed
 * context.
 */
public final class DistributedLock {

  private static final Logger log = Logger.getLogger(DistributedLock.class.getName());

  private final String key;
  private final String identity;
  private final DistributedLockExecutor executor;
  private final Duration until;
  private final KeyValueStore store;
  private ScheduledFuture<?> future;

  private DistributedLock(
      final String key,
      final String identity,
      final Duration delay,
      final Duration until,
      final int tries,
      final KeyValueStore store) {
    this.key = key;
    this.identity = identity;
    this.until = until;
    this.store = store;
    this.executor = new DistributedLockExecutor(delay, until, tries);
  }

  public static DistributedLock create(
      final String key,
      final String identity,
      final Duration delay,
      final Duration until,
      final int tries,
      final KeyValueStore store) {
    return new DistributedLock(key, identity, delay, until, tries, store);
  }

  public static DistributedLock create(
      final String key, final String identity, final int tries, final KeyValueStore store) {
    return create(key, identity, ofMillis(150L), ofSeconds(3L), tries, store);
  }

  public <T> CompletableFuture<T> supply(final Supplier<T> supplier) {
    return executor.supply(
        () -> {
          final boolean acquired = store.set(key, identity, until, true);
          if (!acquired) {
            throw new DistributedLockException("Lock is already held by another process.");
          }

          try {
            startWatching();
            return supplier.get();
          } finally {
            stopWatching();
            store.del(key);
          }
        });
  }

  public CompletableFuture<Void> execute(final Runnable task) {
    return executor.execute(
        () -> {
          final boolean acquired = store.set(key, identity, until, true);
          if (!acquired) {
            throw new DistributedLockException("Lock is already held by another process.");
          }

          try {
            startWatching();
            task.run();
          } finally {
            stopWatching();
            store.del(key);
          }
        });
  }

  public CompletableFuture<Boolean> tryExecuteOnce(final Runnable task) {
    return supplyAsync(
        () -> {
          final boolean acquired = store.set(key, identity, until, true);
          if (!acquired) {
            return false;
          }

          try {
            startWatching();
            task.run();
            return true;
          } finally {
            stopWatching();
            store.del(key);
          }
        });
  }

  private void startWatching() {
    future =
        SCHEDULER.scheduleAtFixedRate(
            () -> {
              try {
                store.ttl(key, now().plus(until));
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
