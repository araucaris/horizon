package dev.horizon.packet;

import dev.horizon.HorizonException;

public class PacketException extends HorizonException {

  public PacketException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public PacketException(final String message) {
    super(message);
  }
}
