package dev.araucaris.horizon.distributed;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Objects;

public final class DistributedLockContext {
  private String owner;
  private Long expiresAt;

  @JsonCreator
  private DistributedLockContext() {}

  public DistributedLockContext(String owner, Long expiresAt) {
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
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (obj == null || obj.getClass() != getClass()) return false;
    var that = (DistributedLockContext) obj;
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
