package io.mikeamiry.aegis.broker;

/**
 * Represents an exception that may be thrown by the PacketBroker or related classes when an
 * unexpected issue occurs. This exception extends {@link IllegalStateException} and is a
 * specialized runtime exception.
 *
 * <p>PacketBrokerException is used to signal problems specific to packet brokering operations, such
 * as: - Publishing a packet fails. - Observing a channel or subscribing to events encounters an
 * error. - Delegating packets between components fails due to missing or incorrect data.
 *
 * <p>This exception facilitates consistent error handling when working with classes in the packet
 * architecture by providing actionable messages and optional causes for easier debugging.
 */
public final class PacketBrokerException extends IllegalStateException {

  PacketBrokerException(final String message) {
    super(message);
  }

  PacketBrokerException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
