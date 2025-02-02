package dev.horizon.lock;

import static com.spotify.futures.CompletableFutures.exceptionallyCompose;
import static dev.horizon.lock.DistributedLockUtils.runLater;
import static java.lang.Math.min;
import static java.time.Duration.ZERO;
import static java.time.Duration.ofMillis;
import static java.time.Instant.now;
import static java.time.Instant.ofEpochMilli;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.ThreadLocalRandom.current;

import dev.horizon.cache.HorizonCache;
import dev.horizon.cache.HorizonCacheException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

public class DistributedLock {

  private final String key;
  private final String pid;
  private final HorizonCache cache;

  public DistributedLock(final String key, final HorizonCache cache) {
    this.key = key;
    this.pid = randomUUID().toString();
    this.cache = cache;
  }

  public boolean acquire(final Duration ttl) throws DistributedLockException {
    final String lockKey = getLockKey();
    try {
      final Instant now = now();
      final Instant expiresAt = now.plus(ttl);

      final DistributedLockContext existingContext =
          cache.get(lockKey, DistributedLockContext.class);
      if (existingContext == null || now.isAfter(ofEpochMilli(existingContext.expiresAt()))) {
        return cache.set(lockKey, new DistributedLockContext(pid, expiresAt.toEpochMilli()));
      }

      return false;
    } catch (final HorizonCacheException exception) {
      throw new DistributedLockException(
          "Failed to acquire lock %s with ttl %s".formatted(lockKey, ttl), exception);
    }
  }

  public boolean release() throws DistributedLockException {
    final String lockKey = getLockKey();
    try {
      final DistributedLockContext context = cache.get(lockKey, DistributedLockContext.class);
      if (context == null) {
        return false;
      }

      if (context.owner().equals(pid)) {
        cache.remove(lockKey);
        return true;
      }

      return false;
    } catch (final HorizonCacheException exception) {
      throw new DistributedLockException("Failed to release lock %s".formatted(lockKey), exception);
    }
  }

  public CompletableFuture<Void> execute(
      final Runnable task, final Duration delay, final Duration until) {
    return execute(task, 0, delay, until, ZERO, now().plus(until));
  }

  private CompletableFuture<Void> execute(
      final Runnable action,
      final int retryCount,
      final Duration delay,
      final Duration until,
      final Duration backoffDelay,
      final Instant untilTime) {
    if (now().isAfter(untilTime)) {
      throw new RetryingException(retryCount);
    }
    return exceptionallyCompose(
            runLater(
                () -> {
                  if (!acquire(until)) {
                    throw new IllegalStateException(
                        "Failed to acquire lock within the specified time.");
                  }
                  action.run();
                  release();
                },
                calculateBackoffDelay(delay, until, retryCount + 1)),
            cause ->
                execute(
                    action,
                    retryCount + 1,
                    delay,
                    until,
                    backoffDelay.plus(calculateBackoffDelay(delay, until, retryCount + 1)),
                    untilTime))
        .toCompletableFuture();
  }

  private String getLockKey() {
    return "lock-" + key;
  }

  private Duration calculateBackoffDelay(
      final Duration delay, final Duration until, final int retryCount) {
    final long exponentialDelayMillis =
        min(delay.toMillis() * (1L << retryCount), until.toMillis());
    final long randomPart =
        exponentialDelayMillis / 2 + current().nextInt((int) (exponentialDelayMillis / 2));
    return ofMillis(randomPart);
  }
}
