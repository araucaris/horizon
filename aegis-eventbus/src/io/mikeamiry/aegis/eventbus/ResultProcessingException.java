package io.mikeamiry.aegis.eventbus;

public final class ResultProcessingException extends IllegalStateException {

  ResultProcessingException(final String message) {
    super(message);
  }

  ResultProcessingException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
