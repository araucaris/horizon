package io.mikeamiry.aegis.eventbus;

/**
 * Represents an observer in the event-driven system.
 *
 * <p>An observer is an entity that listens for specific events published within the system. Classes
 * implementing this interface can define methods annotated with {@link Observe} to handle events
 * fired through the {@link EventBus}. Each observer can optionally specify a topic, which can be
 * used to filter event handling based on the topic provided during event publishing.
 *
 * <p>Key Characteristics: - Observers must implement this interface to be eligible for registration
 * with the {@link EventBus}. - Observers can define event-handling logic in methods marked with
 * {@link Observe}. - The optional topic mechanism enables selective event handling for targeted
 * subscriptions.
 *
 * <p>Thread Safety: - Observers should ensure thread safety of their methods if the {@link
 * EventBus} execution model operates in a multi-threaded environment.
 *
 * <p>Default Methods: - The default method `topic()` can be overridden to specify a unique topic
 * for this observer. If left unimplemented, it defaults to an empty string, indicating no specific
 * topic.
 */
public interface Observer {

  default String topic() {
    return "";
  }
}
