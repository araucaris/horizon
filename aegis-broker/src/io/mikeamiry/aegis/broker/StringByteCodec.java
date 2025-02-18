package io.mikeamiry.aegis.broker;

import static java.nio.ByteBuffer.wrap;
import static java.nio.charset.StandardCharsets.UTF_8;

import io.lettuce.core.codec.RedisCodec;
import java.nio.ByteBuffer;

final class StringByteCodec implements RedisCodec<String, byte[]> {

  @Override
  public String decodeKey(final ByteBuffer bytes) {
    return UTF_8.decode(bytes).toString();
  }

  @Override
  public byte[] decodeValue(final ByteBuffer bytes) {
    final byte[] array = new byte[bytes.remaining()];
    bytes.get(array);
    return array;
  }

  @Override
  public ByteBuffer encodeKey(final String key) {
    return UTF_8.encode(key);
  }

  @Override
  public ByteBuffer encodeValue(final byte[] value) {
    return wrap(value);
  }
}
