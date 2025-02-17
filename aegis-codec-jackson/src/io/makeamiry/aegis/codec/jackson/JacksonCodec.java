package io.makeamiry.aegis.codec.jackson;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.*;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY;
import static com.fasterxml.jackson.core.JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping.NON_FINAL_AND_ENUMS;
import static com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS;
import static java.lang.reflect.Modifier.isFinal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import io.makeamiry.aegis.codec.Codec;
import io.makeamiry.aegis.codec.DecodingException;
import io.makeamiry.aegis.codec.EncodingException;

public final class JacksonCodec implements Codec {

  private static final String POLYMORPHIC_SERIALIZATION_ERROR_TEMPLATE =
      """
      The encoding process encountered an unrecoverable error when attempting to encode the provided class: {CLASS_NAME}.
      This issue arises due to the class being declared as `final`, which restricts further extension or inheritance
      and consequently limits the ability of the Jackson library to properly handle polymorphic type serialization.

      Suggested Solutions:
      1. Remove the `final` modifier from the class definition to allow it to participate in polymorphic serialization:
         Example:
         Instead of:
         final class {CLASS_NAME_SIMPLE} {}
         Change it to:
         class {CLASS_NAME_SIMPLE} {}

      2. Alternatively, if modifying the class is not feasible, apply the @JsonTypeInfo annotation to instruct Jackson
         on how to manage type information during serialization and deserialization processes.
         Example:
         @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "type")
         final class {CLASS_NAME_SIMPLE} {}

      Detailed Context:
      The final modifier prevents Jackson's default type handling strategy from injecting or interpreting type
      information, particularly in scenarios where polymorphic deserialization or custom serializers are involved.

      """;
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

  private static void validateTypeModifiers(final Class<?> type) {
    if (isFinal(type.getModifiers())) {
      throw new EncodingException(
          POLYMORPHIC_SERIALIZATION_ERROR_TEMPLATE
              .replace("{CLASS_NAME}", type.getName())
              .replace("{CLASS_NAME_SIMPLE}", type.getSimpleName()));
    }
  }

  @Override
  public <T> String encode(final T instance) throws EncodingException {
    validateTypeModifiers(instance.getClass());
    try {
      return objectMapper.writeValueAsString(instance);
    } catch (final Exception exception) {
      throw new EncodingException(
          "Could not encode %s into json payload, because of unexpected exception."
              .formatted(instance.getClass()),
          exception);
    }
  }

  @Override
  public <T> byte[] encodeToBytes(final T instance) throws EncodingException {
    validateTypeModifiers(instance.getClass());
    try {
      return objectMapper.writeValueAsBytes(instance);
    } catch (final Exception exception) {
      throw new EncodingException(
          "Could not encode %s into binary payload, because of unexpected exception."
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
          "Could not decode json payload into instance, because of unexpected exception.",
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
          "Could not decode binary payload into instance, because of unexpected exception.",
          exception);
    }
  }
}
