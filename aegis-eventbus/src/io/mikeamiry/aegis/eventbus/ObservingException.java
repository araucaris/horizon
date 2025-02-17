package io.mikeamiry.aegis.eventbus;

public final class ObservingException extends RuntimeException {

  ObservingException(final String message) {
    super(message);
  }

  ObservingException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
