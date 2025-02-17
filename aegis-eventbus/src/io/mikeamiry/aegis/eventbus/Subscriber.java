package io.mikeamiry.aegis.eventbus;

public interface Subscriber {

  default String topic() {
    return "";
  }

  default boolean observer() {
    return false;
  }
}
