package io.mikeamiry.aegis.eventbus;

public sealed interface EventBus permits EventBusImpl {

  void observe(Observer observer) throws ObservingException;

  void publish(Event event, String... targets) throws EventPublishingException;

  <E extends Event, T> void register(Class<T> resultType, ResultProcessor<E, T> resultProcessor);
}
