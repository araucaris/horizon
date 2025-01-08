package dev.araucaris.horizon.packet.callback;

import dev.araucaris.horizon.HorizonListener;
import dev.araucaris.horizon.packet.Packet;
import dev.araucaris.horizon.serdes.HorizonSerdes;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class PacketCallbackListener implements HorizonListener {

  private final HorizonSerdes horizonSerdes;
  private final PacketCallbackCache packetCallbackCache;

  private PacketCallbackListener(
      HorizonSerdes horizonSerdes, PacketCallbackCache packetCallbackCache) {
    this.horizonSerdes = horizonSerdes;
    this.packetCallbackCache = packetCallbackCache;
  }

  public static PacketCallbackListener create(
      HorizonSerdes horizonSerdes, PacketCallbackCache packetCallbackCache) {
    return new PacketCallbackListener(horizonSerdes, packetCallbackCache);
  }

  @Override
  public void handle(String channelName, byte[] payload) {
    Packet packet = horizonSerdes.decode(payload, Packet.class);
    if (packet == null) {
      return;
    }

    UUID uniqueId = packet.getUniqueId();
    CompletableFuture<?> requestToComplete = packetCallbackCache.findByUniqueId(uniqueId);
    if (requestToComplete == null) {
      return;
    }

    packetCallbackCache.remove(uniqueId);

    try {
      //noinspection unchecked
      ((CompletableFuture<Packet>) requestToComplete).complete(packet);
    } catch (Exception exception) {
      throw new PacketCallbackException(
          "Failed to complete packet identified by %s".formatted(uniqueId), exception);
    }
  }
}
