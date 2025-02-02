package dev.horizon;

@FunctionalInterface
public interface HorizonListener {

  void receive(String topic, byte[] payload);
}
