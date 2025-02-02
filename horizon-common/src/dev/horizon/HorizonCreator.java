package dev.horizon;

import static java.time.Duration.ofSeconds;

import dev.horizon.codec.HorizonCodec;
import dev.horizon.codec.jackson.JacksonCodecFactory;
import io.lettuce.core.RedisClient;
import java.time.Duration;
import java.util.function.Supplier;

public final class HorizonCreator {

  private final RedisClient redisClient;
  private Supplier<HorizonCodec> codec = JacksonCodecFactory::getJacksonCodec;
  private Supplier<Duration> requestCleanupInterval = () -> ofSeconds(10L);

  private HorizonCreator(final RedisClient redisClient) {
    this.redisClient = redisClient;
  }

  public static HorizonCreator creator(final RedisClient redisClient) {
    return new HorizonCreator(redisClient);
  }

  public HorizonCreator requestCleanupInterval(final Duration requestCleanupInterval) {
    this.requestCleanupInterval = () -> requestCleanupInterval;
    return this;
  }

  public HorizonCreator codec(final HorizonCodec horizonCodec) {
    this.codec = () -> horizonCodec;
    return this;
  }

  public Horizon create() {
    return new Horizon(redisClient, codec.get(), requestCleanupInterval.get());
  }
}
