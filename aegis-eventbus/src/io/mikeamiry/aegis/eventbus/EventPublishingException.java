package io.mikeamiry.aegis.eventbus;

public final class EventPublishingException extends RuntimeException {

  EventPublishingException(final String message, final Throwable cause) {
    super(message, cause);
  }

  EventPublishingException(final String message) {
    super(message);
  }
}
