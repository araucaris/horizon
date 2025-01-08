package dev.araucaris.horizon.serdes;

public interface HorizonSerdes {

  <T> T decode(byte[] payload, Class<T> type) throws SerdesException;

  byte[] encode(Object value) throws SerdesException;
}
