package dev.horizon.lock;

import java.util.Objects;

public final class DistributedLockContext {
  private String owner;
  private Long expiresAt;

  @SuppressWarnings("unused")
  public DistributedLockContext() {}

  public DistributedLockContext(final String owner, final Long expiresAt) {
    this.owner = owner;
    this.expiresAt = expiresAt;
  }

  public String owner() {
    return owner;
  }

  public Long expiresAt() {
    return expiresAt;
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == this) return true;
    if (obj == null || obj.getClass() != getClass()) return false;
    final var that = (DistributedLockContext) obj;
    return Objects.equals(owner, that.owner) && Objects.equals(expiresAt, that.expiresAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(owner, expiresAt);
  }

  @Override
  public String toString() {
    return "DistributedLockContext[" + "owner=" + owner + ", " + "expiresAt=" + expiresAt + ']';
  }
}
