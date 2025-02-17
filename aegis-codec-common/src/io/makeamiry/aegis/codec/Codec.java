package io.makeamiry.aegis.codec;

public interface Codec {

  <T> String encode(final T instance) throws EncodingException;

  <T> byte[] encodeToBytes(final T instance) throws EncodingException;

  <T> T decode(final String payload) throws DecodingException;

  default <T> T decode(final String payload, final Class<T> type) throws DecodingException {
    return decode(payload);
  }

  <T> T decodeFromBytes(final byte[] payload) throws DecodingException;

  default <T> T decodeFromBytes(final byte[] payload, final Class<T> type)
      throws DecodingException {
    return decodeFromBytes(payload);
  }
}
