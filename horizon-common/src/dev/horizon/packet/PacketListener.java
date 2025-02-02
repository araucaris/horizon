package dev.horizon.packet;

import dev.horizon.HorizonListener;
import io.lettuce.core.pubsub.RedisPubSubListener;

public final class PacketListener implements RedisPubSubListener<String, byte[]> {

  private final String subscribedTopic;
  private final HorizonListener listener;

  private PacketListener(final String subscribedTopic, final HorizonListener listener) {
    this.subscribedTopic = subscribedTopic;
    this.listener = listener;
  }

  public static PacketListener create(
      final String subscribedTopic, final HorizonListener listener) {
    return new PacketListener(subscribedTopic, listener);
  }

  @Override
  public void message(final String channelName, final byte[] message) {
    if (subscribedTopic.equals(channelName)) {
      listener.receive(channelName, message);
    }
  }

  @Override
  public void message(final String pattern, final String channelName, final byte[] message) {
    message("%s:%s".formatted(pattern, channelName), message);
  }

  @Override
  public void subscribed(final String channel, final long count) {}

  @Override
  public void psubscribed(final String pattern, final long count) {}

  @Override
  public void unsubscribed(final String channel, final long count) {}

  @Override
  public void punsubscribed(final String pattern, final long count) {}
}
