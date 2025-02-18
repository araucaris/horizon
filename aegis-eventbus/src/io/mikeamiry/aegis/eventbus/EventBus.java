package io.mikeamiry.aegis.eventbus;

/**
 * Represents the core interface for the EventBus system, enabling event-based communication between
 * different parts of an application. It supports observation of events, event publishing, and
 * result processing.
 *
 * <p>This interface is a sealed type that can only be implemented by specific permitted classes.
 */
public sealed interface EventBus permits EventBusImpl {

  void observe(Observer observer) throws ObservingException;

  void publish(Event event, String... targets) throws EventPublishingException;

  <E extends Event, T> void register(Class<T> resultType, ResultProcessor<E, T> resultProcessor);
}
