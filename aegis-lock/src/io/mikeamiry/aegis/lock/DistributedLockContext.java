package io.mikeamiry.aegis.lock;

import static java.lang.Long.parseLong;

import java.util.Objects;

/**
 * Represents the context of a distributed lock, holding metadata about the lock owner (identity)
 * and the lock's expiration timestamp.
 *
 * <p>The {@code DistributedLockContext} class is immutable for the identity, but allows updating
 * the expiration timestamp. It is utilized within distributed lock mechanisms to ensure proper
 * identification and lifecycle management of the lock.
 *
 * <p>Core responsibilities: - Encapsulates the identity of the lock owner as a string. - Stores and
 * provides access to the expiration timestamp of the lock. - Provides parsing functionality to
 * reconstruct a {@code DistributedLockContext} instance from its serialized string representation.
 *
 * <p>Features: - Overrides {@code equals} and {@code hashCode} based on the identity, ensuring that
 * comparisons and hash-based data structure operations work appropriately. - Overrides {@code
 * toString} to produce a serialized representation of the instance.
 *
 * <p>Usage notes: This class is designed for use in distributed systems where locks are persisted
 * in a shared storage (e.g., a key-value store). Its serialized format is intended to be compact
 * and includes the lock owner’s identity and expiration timestamp, separated by a colon (":").
 *
 * <p>Parsing: The {@link #parse(String)} method reconstructs a {@code DistributedLockContext} from
 * its serialized form. The method will throw {@code DistributedLockException} if the input is
 * malformed or does not conform to the expected format.
 */
public final class DistributedLockContext {

  private final String identity;
  private Long expiresAt;

  public DistributedLockContext(final String identity, final Long expiresAt) {
    this.identity = identity;
    this.expiresAt = expiresAt;
  }

  public static DistributedLockContext parse(final String payload) {
    final String[] parts = payload.split(":");
    if (parts.length != 2) {
      throw new DistributedLockException("Invalid serialized lock context (%s)".formatted(payload));
    }
    return new DistributedLockContext(parts[0], parseLong(parts[1]));
  }

  public String identity() {
    return identity;
  }

  public Long expiresAt() {
    return expiresAt;
  }

  public void expiresAt(final Long expiresAt) {
    this.expiresAt = expiresAt;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(identity);
  }

  @Override
  public boolean equals(final Object o) {
    if (o == null || getClass() != o.getClass()) return false;

    final DistributedLockContext that = (DistributedLockContext) o;
    return Objects.equals(identity, that.identity);
  }

  @Override
  public String toString() {
    return identity + ":" + expiresAt;
  }
}
