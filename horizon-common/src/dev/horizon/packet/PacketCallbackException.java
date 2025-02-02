package dev.horizon.packet;

public final class PacketCallbackException extends PacketException {

  public PacketCallbackException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public PacketCallbackException(final String message) {
    super(message);
  }
}
