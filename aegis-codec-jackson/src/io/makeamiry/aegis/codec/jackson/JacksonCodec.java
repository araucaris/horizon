package io.makeamiry.aegis.codec.jackson;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.*;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY;
import static com.fasterxml.jackson.core.JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping.NON_FINAL_AND_ENUMS;
import static com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import io.makeamiry.aegis.codec.Codec;
import io.makeamiry.aegis.codec.DecodingException;
import io.makeamiry.aegis.codec.EncodingException;

public final class JacksonCodec implements Codec {
  private final ObjectMapper objectMapper;

  private JacksonCodec(final ObjectMapper mapperToCopy) {
    this.objectMapper = mapperToCopy.copy();
    objectMapper.setVisibility(
        objectMapper
            .getSerializationConfig()
            .getDefaultVisibilityChecker()
            .withFieldVisibility(ANY)
            .withGetterVisibility(NONE)
            .withSetterVisibility(NONE)
            .withCreatorVisibility(NONE));
    objectMapper.disable(FAIL_ON_UNKNOWN_PROPERTIES);
    objectMapper.disable(FAIL_ON_EMPTY_BEANS);
    objectMapper.enable(WRITE_BIGDECIMAL_AS_PLAIN);
    objectMapper.activateDefaultTyping(
        BasicPolymorphicTypeValidator.builder().allowIfBaseType("java").build(),
        NON_FINAL_AND_ENUMS,
        PROPERTY);
  }

  public static JacksonCodec create(final ObjectMapper objectMapper) {
    return new JacksonCodec(objectMapper);
  }

  public static JacksonCodec create() {
    return create(new ObjectMapper());
  }

  @Override
  public <T> String encode(final T instance) throws EncodingException {
    try {
      return objectMapper.writeValueAsString(instance);
    } catch (final Exception exception) {
      throw new EncodingException(
          "Could not serialize %s into json payload, because of unexpected exception."
              .formatted(instance.getClass()),
          exception);
    }
  }

  @Override
  public <T> byte[] encodeToBytes(final T instance) throws EncodingException {
    try {
      return objectMapper.writeValueAsBytes(instance);
    } catch (final Exception exception) {
      throw new EncodingException(
          "Could not serialize %s into binary payload, because of unexpected exception."
              .formatted(instance.getClass()),
          exception);
    }
  }

  @Override
  public <T> T decode(final String payload) throws DecodingException {
    try {
      // noinspection unchecked
      return (T) objectMapper.readValue(payload, Object.class);
    } catch (final Exception exception) {
      throw new DecodingException(
          "Could not deserialize json payload into instance, because of unexpected exception.",
          exception);
    }
  }

  @Override
  public <T> T decodeFromBytes(final byte[] payload) throws DecodingException {
    try {
      // noinspection unchecked
      return (T) objectMapper.readValue(payload, Object.class);
    } catch (final Exception exception) {
      throw new DecodingException(
          "Could not deserialize binary payload into instance, because of unexpected exception.",
          exception);
    }
  }
}
