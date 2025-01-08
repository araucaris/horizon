package dev.araucaris.horizon.packet.callback;

import dev.araucaris.horizon.packet.PacketException;

public final class PacketCallbackException extends PacketException {

  public PacketCallbackException(String message, Throwable cause) {
    super(message, cause);
  }
}
