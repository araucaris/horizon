package dev.araucaris.horizon.distributed;

import static com.spotify.futures.CompletableFutures.exceptionallyCompose;
import static dev.araucaris.horizon.distributed.DistributedLockUtils.runLater;
import static java.lang.Math.min;
import static java.time.Duration.ZERO;
import static java.time.Duration.ofMillis;
import static java.time.Instant.now;
import static java.time.Instant.ofEpochMilli;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.ThreadLocalRandom.current;

import dev.araucaris.horizon.storage.HorizonStorage;
import dev.araucaris.horizon.storage.StorageException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

public class DistributedLock {

  private final String key;
  private final String pid;
  private final HorizonStorage storage;

  public DistributedLock(String key, HorizonStorage storage) {
    this.key = key;
    this.pid = randomUUID().toString();
    this.storage = storage;
  }

  public boolean acquire(Duration ttl) throws DistributedLockException {
    String lockKey = getLockKey();
    try {
      Instant now = now();
      Instant expiresAt = now.plus(ttl);

      DistributedLockContext existingContext = storage.get(lockKey, DistributedLockContext.class);
      if (existingContext == null || now.isAfter(ofEpochMilli(existingContext.expiresAt()))) {
        return storage.set(lockKey, new DistributedLockContext(pid, expiresAt.toEpochMilli()));
      }

      return false;
    } catch (StorageException exception) {
      throw new DistributedLockException(
          "Failed to acquire lock %s with ttl %s".formatted(lockKey, ttl), exception);
    }
  }

  public boolean release() throws DistributedLockException {
    String lockKey = getLockKey();
    try {
      DistributedLockContext context = storage.get(lockKey, DistributedLockContext.class);
      if (context == null) {
        return false;
      }

      if (context.owner().equals(pid)) {
        storage.remove(lockKey);
        return true;
      }

      return false;
    } catch (StorageException exception) {
      throw new DistributedLockException("Failed to release lock %s".formatted(lockKey), exception);
    }
  }

  public CompletableFuture<Void> execute(Runnable task, Duration delay, Duration until) {
    return execute(task, 0, delay, until, ZERO, now().plus(until));
  }

  private CompletableFuture<Void> execute(
      Runnable action,
      int retryCount,
      Duration delay,
      Duration until,
      Duration backoffDelay,
      Instant untilTime) {
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

  private Duration calculateBackoffDelay(Duration delay, Duration until, int retryCount) {
    long exponentialDelayMillis = min(delay.toMillis() * (1L << retryCount), until.toMillis());
    long randomPart =
        exponentialDelayMillis / 2 + current().nextInt((int) (exponentialDelayMillis / 2));
    return ofMillis(randomPart);
  }
}
