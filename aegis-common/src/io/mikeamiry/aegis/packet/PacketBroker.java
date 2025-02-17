package io.mikeamiry.aegis.packet;

import io.mikeamiry.aegis.eventbus.Subscriber;
import java.util.concurrent.CompletableFuture;

/**
 * The PacketBroker interface serves as an abstraction for a packet-based communication broker,
 * enabling operations such as observing subscribers, publishing packets to specific channels, and
 * handling synchronous packet requests.
 *
 * <p>This interface is sealed, allowing only specified classes to implement it. The primary
 * implementation is provided by the PacketBrokerImpl class.
 *
 * <p>Key responsibilities include: - Observing subscribers for specific communication or event
 * channels. - Publishing packets to designated channels for delivery to subscribers. - Facilitating
 * request-response communication with packets.
 */
public sealed interface PacketBroker permits PacketBrokerImpl {

  void observe(final Subscriber subscriber) throws PacketBrokerException;

  void publish(final String channel, final Packet packet) throws PacketBrokerException;

  <T extends Packet> CompletableFuture<T> request(final String channel, final Packet request);
}
