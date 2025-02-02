package dev.horizon.packet;

import dev.horizon.HorizonListener;
import dev.horizon.codec.HorizonCodec;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class PacketCallbackListener implements HorizonListener {

  private final HorizonCodec horizonCodec;
  private final PacketCallbackCache packetCallbackCache;

  public PacketCallbackListener(
      final HorizonCodec horizonCodec, final PacketCallbackCache packetCallbackCache) {
    this.horizonCodec = horizonCodec;
    this.packetCallbackCache = packetCallbackCache;
  }

  @Override
  public void receive(final String topic, final byte[] payload) {
    final Packet packet = horizonCodec.decode(payload, Packet.class);
    if (packet == null) {
      return;
    }

    final UUID uniqueId = packet.getUniqueId();
    final CompletableFuture<?> requestToComplete = packetCallbackCache.findByUniqueId(uniqueId);
    if (requestToComplete == null) {
      return;
    }

    packetCallbackCache.remove(uniqueId);

    try {
      //noinspection unchecked
      ((CompletableFuture<Packet>) requestToComplete).complete(packet);
    } catch (final Exception exception) {
      throw new PacketCallbackException(
          "Failed to complete packet identified by %s".formatted(uniqueId), exception);
    }
  }
}
