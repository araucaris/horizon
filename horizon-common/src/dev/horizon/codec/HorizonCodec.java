package dev.horizon.codec;

public interface HorizonCodec {

  <T> T decode(byte[] payload, Class<T> type) throws HorizonCodecException;

  byte[] encode(Object value) throws HorizonCodecException;
}
