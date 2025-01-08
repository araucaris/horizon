package dev.araucaris.horizon;

@FunctionalInterface
public interface HorizonListener {

  void handle(String channelName, byte[] payload);
}
