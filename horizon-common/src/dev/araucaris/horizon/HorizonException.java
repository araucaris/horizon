package dev.araucaris.horizon;

public class HorizonException extends IllegalArgumentException {

  HorizonException(String message) {
    super(message);
  }

  HorizonException(String message, Throwable cause) {
    super(message, cause);
  }
}
