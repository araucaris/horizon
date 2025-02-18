package io.mikeamiry.aegis.eventbus;

import java.util.concurrent.Executor;

/**
 * A factory class for creating instances of {@link EventBus}.
 *
 * <p>This class provides a convenient method to instantiate the {@link EventBus} with its default
 * implementation {@link EventBusImpl}. The default implementation utilizes a combination of {@link
 * ObservationService} for handling event observers and {@link ResultProcessorService} for managing
 * result processing of events.
 *
 * <p>This factory ensures the encapsulation of the underlying details of the {@link EventBus}
 * implementation, providing a simplified and consistent way to create instances of the event bus.
 *
 * <p>This class is final and cannot be extended. It operates as a utility class and is not intended
 * to be instantiated.
 */
public final class EventBusFactory {

  private EventBusFactory() {}

  public static EventBus create(final Executor executor) {
    return new EventBusImpl(executor, new ObservationService(), new ResultProcessorService());
  }
}
