package io.makeamiry.aegis.codec;

/**
 * An exception that is thrown when an error occurs during the encoding process. This class extends
 * IllegalStateException to indicate an illegal state specific to encoding operations.
 */
public final class EncodingException extends IllegalStateException {

  public EncodingException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public EncodingException(final String message) {
    super(message);
  }
}
