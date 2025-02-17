package io.mikeamiry.aegis.eventbus;

public interface Observer {

  default String topic() {
    return "";
  }
}
