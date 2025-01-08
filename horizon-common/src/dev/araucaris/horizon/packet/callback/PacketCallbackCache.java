package dev.araucaris.horizon.packet.callback;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class PacketCallbackCache {

  private final Map<UUID, CompletableFuture<?>> responses = new ConcurrentHashMap<>();

  private PacketCallbackCache() {}

  public static PacketCallbackCache create() {
    return new PacketCallbackCache();
  }

  public void add(UUID uniqueId, CompletableFuture<?> responseFuture) {
    responses.put(uniqueId, responseFuture);
  }

  public void remove(UUID uniqueId) {
    responses.remove(uniqueId);
  }

  public CompletableFuture<?> findByUniqueId(UUID uniqueId) {
    return responses.get(uniqueId);
  }
}
