package dev.araucaris.horizon;

import static java.time.Duration.ofSeconds;

import dev.araucaris.horizon.serdes.HorizonSerdes;
import dev.araucaris.horizon.serdes.jackson.JacksonSerdesFactory;
import io.lettuce.core.RedisClient;
import java.time.Duration;
import java.util.function.Supplier;

public final class HorizonBuilder {

  private final RedisClient redisClient;
  private Supplier<HorizonSerdes> horizonSerdes = JacksonSerdesFactory::getJacksonSerdes;
  private Supplier<Duration> requestCleanupInterval = () -> ofSeconds(10L);

  private HorizonBuilder(RedisClient redisClient) {
    this.redisClient = redisClient;
  }

  public static HorizonBuilder newBuilder(RedisClient redisClient) {
    return new HorizonBuilder(redisClient);
  }

  public HorizonBuilder requestCleanupInterval(Duration requestCleanupInterval) {
    this.requestCleanupInterval = () -> requestCleanupInterval;
    return this;
  }

  public HorizonBuilder serdes(HorizonSerdes horizonSerdes) {
    this.horizonSerdes = () -> horizonSerdes;
    return this;
  }

  public Horizon build() {
    return new Horizon(redisClient, horizonSerdes.get(), requestCleanupInterval.get());
  }
}
