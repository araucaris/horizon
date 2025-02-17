package io.makeamiry.aegis.codec;

/**
 * An exception that is thrown when an error occurs during the decoding process. This class extends
 * IllegalStateException to indicate an illegal state specific to decoding operations.
 */
public final class DecodingException extends IllegalStateException {

  public DecodingException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public DecodingException(final String message) {
    super(message);
  }
}
