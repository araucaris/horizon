package io.mikeamiry.aegis.broker;

import io.lettuce.core.RedisClient;
import io.makeamiry.aegis.codec.Codec;
import io.mikeamiry.aegis.eventbus.EventBus;

/**
 * A factory class for creating instances of the {@code PacketBroker} interface.
 *
 * <p>This factory provides an abstraction for initializing the primary implementation of {@code
 * PacketBroker}, specifically the {@code PacketBrokerImpl}. The created broker facilitates
 * packet-based communication by integrating the provided codec, event bus, and Redis client for
 * message handling and distribution.
 *
 * <p>Responsibilities of this factory include: - Abstracting the instantiation of a {@code
 * PacketBroker} implementation. - Ensuring that necessary dependencies like {@code Codec}, {@code
 * EventBus}, and {@code RedisClient} are correctly injected into the broker. - Returning a fully
 * initialized instance of {@code PacketBroker} for usage.
 *
 * <p>This class is final to ensure immutability and prevent extension. It uses a private
 * constructor to enforce static factory methods as the only entry point.
 */
public final class PacketBrokerFactory {

  private PacketBrokerFactory() {}

  public static PacketBroker create(
      final String identity,
      final Codec codec,
      final EventBus eventBus,
      final RedisClient redisClient) {
    return new PacketBrokerImpl(identity, codec, eventBus, redisClient);
  }
}
