package dev.horizon;

public class HorizonException extends IllegalArgumentException {

  public HorizonException(final String message) {
    super(message);
  }

  public HorizonException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
