package io.mikeamiry.aegis.packet;

import io.lettuce.core.pubsub.RedisPubSubListener;
import java.util.function.Consumer;

final class PacketDelegate implements RedisPubSubListener<String, byte[]> {

  private final Consumer<byte[]> messageConsumer;

  PacketDelegate(final Consumer<byte[]> messageConsumer) {
    this.messageConsumer = messageConsumer;
  }

  @Override
  public void message(final String channel, final byte[] message) {
    messageConsumer.accept(message);
  }

  @Override
  public void message(final String pattern, final String channel, final byte[] message) {}

  @Override
  public void subscribed(final String channel, final long count) {}

  @Override
  public void psubscribed(final String pattern, final long count) {}

  @Override
  public void unsubscribed(final String channel, final long count) {}

  @Override
  public void punsubscribed(final String pattern, final long count) {}
}
