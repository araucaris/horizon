package dev.horizon.packet;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class PacketCallbackCache {

  private final Map<UUID, CompletableFuture<?>> responses = new ConcurrentHashMap<>();

  public void add(final UUID uniqueId, final CompletableFuture<?> responseFuture) {
    responses.put(uniqueId, responseFuture);
  }

  public void remove(final UUID uniqueId) {
    responses.remove(uniqueId);
  }

  public CompletableFuture<?> findByUniqueId(final UUID uniqueId) {
    return responses.get(uniqueId);
  }
}
