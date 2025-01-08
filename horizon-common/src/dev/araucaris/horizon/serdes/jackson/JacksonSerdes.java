package dev.araucaris.horizon.serdes.jackson;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import dev.araucaris.horizon.serdes.HorizonSerdes;
import dev.araucaris.horizon.serdes.SerdesException;
import java.util.Arrays;

final class JacksonSerdes implements HorizonSerdes {

  private final ObjectMapper mapper;

  JacksonSerdes(ObjectMapper mapper) {
    this.mapper =
        mapper
            .copy()
            .activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);
  }

  @Override
  public <T> T decode(byte[] payload, Class<T> type) throws SerdesException {
    if (payload == null) {
      return null;
    }
    try {
      return mapper.readValue(payload, type);
    } catch (InvalidTypeIdException exception) {
      throw new SerdesException(
          "Type %s could not be found, when deserializing via Jackson"
              .formatted(exception.getTypeId()),
          exception);
    } catch (Exception exception) {
      throw new SerdesException(
          "Could not deserialize via Jackson, preview %s".formatted(Arrays.toString(payload)),
          exception);
    }
  }

  @Override
  public byte[] encode(Object value) throws SerdesException {
    if (value == null) {
      return null;
    }
    try {
      return mapper.writeValueAsBytes(value);
    } catch (Exception exception) {
      throw new SerdesException(
          "Could not serialize %s via Jackson".formatted(value.getClass()), exception);
    }
  }
}
