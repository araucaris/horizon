package dev.horizon.codec;

import dev.horizon.HorizonException;

public class HorizonCodecException extends HorizonException {

  public HorizonCodecException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public HorizonCodecException(final String message) {
    super(message);
  }
}
