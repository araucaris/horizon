package io.mikeamiry.aegis.eventbus;

public final class EventBusFactory {

  private EventBusFactory() {}

  public static EventBus create() {
    return new EventBusImpl(new ObservationService(), new ResultProcessorService());
  }
}
