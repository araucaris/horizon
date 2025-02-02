package dev.horizon.codec.jackson;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import dev.horizon.codec.HorizonCodec;
import dev.horizon.codec.HorizonCodecException;
import java.util.Arrays;

final class JacksonCodec implements HorizonCodec {

  private final ObjectMapper mapper;

  JacksonCodec(final ObjectMapper mapper) {
    this.mapper =
        mapper
            .copy()
            .activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);
  }

  @Override
  public <T> T decode(final byte[] payload, final Class<T> type) throws HorizonCodecException {
    if (payload == null) {
      return null;
    }
    try {
      return mapper.readValue(payload, type);
    } catch (final InvalidTypeIdException exception) {
      throw new HorizonCodecException(
          "Type %s could not be found, when deserializing via Jackson"
              .formatted(exception.getTypeId()),
          exception);
    } catch (final Exception exception) {
      throw new HorizonCodecException(
          "Could not deserialize via Jackson, preview %s".formatted(Arrays.toString(payload)),
          exception);
    }
  }

  @Override
  public byte[] encode(final Object value) throws HorizonCodecException {
    if (value == null) {
      return null;
    }
    try {
      return mapper.writeValueAsBytes(value);
    } catch (final Exception exception) {
      throw new HorizonCodecException(
          "Could not serialize %s via Jackson".formatted(value.getClass()), exception);
    }
  }
}
