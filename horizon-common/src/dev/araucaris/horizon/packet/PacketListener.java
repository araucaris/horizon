package dev.araucaris.horizon.packet;

import dev.araucaris.horizon.HorizonListener;
import io.lettuce.core.pubsub.RedisPubSubListener;

public final class PacketListener implements RedisPubSubListener<String, byte[]> {

  private final String subscribedTopic;
  private final HorizonListener listener;

  private PacketListener(String subscribedTopic, HorizonListener listener) {
    this.subscribedTopic = subscribedTopic;
    this.listener = listener;
  }

  public static PacketListener create(String subscribedTopic, HorizonListener listener) {
    return new PacketListener(subscribedTopic, listener);
  }

  @Override
  public void message(String channelName, byte[] message) {
    if (subscribedTopic.equals(channelName)) {
      listener.handle(channelName, message);
    }
  }

  @Override
  public void message(String pattern, String channelName, byte[] message) {
    message("%s:%s".formatted(pattern, channelName), message);
  }

  @Override
  public void subscribed(String channel, long count) {}

  @Override
  public void psubscribed(String pattern, long count) {}

  @Override
  public void unsubscribed(String channel, long count) {}

  @Override
  public void punsubscribed(String pattern, long count) {}
}
