package io.mikeamiry.aegis.eventbus;

/**
 * Represents an exception that occurs during event publishing within the {@link EventBus} system.
 *
 * <p>This exception is typically thrown when an error arises while invoking observer methods or
 * processing an event. It indicates that an issue occurred in the execution flow of handling
 * published events.
 *
 * <p>The {@link EventPublishingException} is a {@code RuntimeException}, allowing it to be used
 * where checked exceptions are not desired. It encapsulates the detailed cause and message
 * describing the failure, aiding in debugging and error resolution.
 *
 * <p>Common scenarios where this exception may be thrown include: - Failures in invoking subscriber
 * methods due to illegal access or incorrect parameters. - Errors within the observer methods, like
 * unhandled exceptions in observer logic. - Other unexpected issues preventing successful event
 * delivery or handling.
 *
 * <p>This class is final and not intended for extension.
 */
public final class EventPublishingException extends RuntimeException {

  EventPublishingException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
