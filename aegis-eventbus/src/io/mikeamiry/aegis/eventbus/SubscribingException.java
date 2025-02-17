package io.mikeamiry.aegis.eventbus;

public final class SubscribingException extends RuntimeException {

  SubscribingException(final String message) {
    super(message);
  }

  SubscribingException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
