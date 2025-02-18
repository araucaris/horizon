package io.mikeamiry.aegis;

import static java.lang.ProcessHandle.current;

import io.lettuce.core.RedisClient;
import io.makeamiry.aegis.codec.Codec;
import io.mikeamiry.aegis.broker.PacketBroker;
import io.mikeamiry.aegis.broker.PacketBrokerFactory;
import io.mikeamiry.aegis.eventbus.EventBus;
import io.mikeamiry.aegis.eventbus.EventBusFactory;

/**
 * A factory class for creating instances of the {@link Aegis} interface.
 *
 * <p>This factory provides a series of overloaded {@code create} methods to initialize and
 * configure {@link Aegis} implementations. It serves as the primary entry point for instantiating
 * an {@link AegisClient} with varying levels of customization based on the passed dependencies.
 *
 * <p>Responsibilities of this factory include: - Creating fully configured instances of {@link
 * AegisClient}, the primary implementation of {@link Aegis}. - Managing the injection of
 * dependencies such as {@link Codec}, {@link EventBus}, {@link RedisClient}, and {@link
 * PacketBroker}. - Abstracting setup complexities to simplify the creation of distributed systems
 * abstractions.
 *
 * <p>This class is declared as {@code final} to prevent extension. It uses a private constructor to
 * ensure that it can only be used via its static factory methods.
 */
public final class AegisFactory {

  private AegisFactory() {}

  public static Aegis create(final Codec codec, final RedisClient redisClient) {
    final String identity = String.valueOf(current().pid());
    final EventBus eventBus = EventBusFactory.create();
    return create(identity, codec, eventBus, redisClient);
  }

  public static Aegis create(
      final String identity,
      final Codec codec,
      final EventBus eventBus,
      final RedisClient redisClient) {
    final PacketBroker packetBroker = PacketBrokerFactory.create(codec, eventBus, redisClient);
    return create(identity, packetBroker, redisClient);
  }

  public static Aegis create(
      final String identity, final PacketBroker packetBroker, final RedisClient redisClient) {
    return new AegisClient(identity, redisClient, packetBroker);
  }
}
