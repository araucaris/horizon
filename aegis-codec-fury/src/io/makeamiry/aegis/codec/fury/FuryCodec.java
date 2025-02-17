package io.makeamiry.aegis.codec.fury;

import static org.apache.fury.config.Language.JAVA;
import static org.apache.fury.logging.LoggerFactory.disableLogging;

import io.makeamiry.aegis.codec.Codec;
import io.makeamiry.aegis.codec.DecodingException;
import io.makeamiry.aegis.codec.EncodingException;
import java.util.Base64;
import org.apache.fury.Fury;
import org.apache.fury.ThreadSafeFury;

public final class FuryCodec implements Codec {

  private final ThreadSafeFury fury;

  private FuryCodec(final ThreadSafeFury fury) {
    this.fury = fury;
  }

  public static FuryCodec create(final ThreadSafeFury fury) {
    return new FuryCodec(fury);
  }

  public static FuryCodec create() {
    disableLogging();
    return create(
        Fury.builder()
            .requireClassRegistration(false)
            .withLanguage(JAVA)
            .buildThreadSafeFuryPool(10, 512));
  }

  @Override
  public <T> String encode(final T instance) throws EncodingException {
    return Base64.getEncoder().encodeToString(encodeToBytes(instance));
  }

  @Override
  public <T> byte[] encodeToBytes(final T instance) throws EncodingException {
    try {
      return fury.serialize(instance);
    } catch (final Exception exception) {
      throw new EncodingException(
          "Could not serialize %s into binary payload, because of unexpected exception."
              .formatted(instance.getClass()),
          exception);
    }
  }

  @Override
  public <T> T decode(final String payload64) throws DecodingException {
    return decodeFromBytes(Base64.getDecoder().decode(payload64));
  }

  @Override
  public <T> T decodeFromBytes(final byte[] payload) throws DecodingException {
    try {
      // noinspection unchecked
      return (T) fury.deserialize(payload);
    } catch (final Exception exception) {
      throw new DecodingException(
          "Could not deserialize binary payload into instance, because of unexpected exception.",
          exception);
    }
  }
}
