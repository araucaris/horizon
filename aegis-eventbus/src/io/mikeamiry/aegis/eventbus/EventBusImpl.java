package io.mikeamiry.aegis.eventbus;

import static java.util.Arrays.stream;

import java.lang.invoke.MethodHandle;
import java.util.concurrent.Executor;

/**
 * Represents the default implementation of the {@link EventBus} interface.
 *
 * <p>This class facilitates event-based communication through observation of events, publishing of
 * events to observers, and result processing. It leverages the {@link ObservationService} to
 * register observers and their methods of interest, and the {@link ResultProcessorService} to
 * handle processing of results generated from event notifications.
 *
 * <p>This implementation ensures that: - Observers can subscribe to specific events and subsequent
 * calls to their annotated methods are handled seamlessly. - Published events are propagated to
 * matching observers based on the event type and optional topic criteria. - Results returned by
 * observer methods can be processed by registered result processors.
 *
 * <p>This class is intended to be used internally within the event-driven architecture and should
 * not be extended or modified externally.
 *
 * <p>Thread-safety and correct operation rely on the underlying services, which manage their
 * respective registries in a manner ensuring data integrity.
 */
final class EventBusImpl implements EventBus {

  private final Executor executor;
  private final ObservationService observationService;
  private final ResultProcessorService resultProcessorService;

  EventBusImpl(
      final Executor executor,
      final ObservationService observationService,
      final ResultProcessorService resultProcessorService) {
    this.executor = executor;
    this.observationService = observationService;
    this.resultProcessorService = resultProcessorService;
  }

  @Override
  public void observe(final Observer observer) throws ObservingException {
    observationService.observe(observer);
  }

  @Override
  public void publish(final Event event, final String... topics) throws EventPublishingException {
    observationService
        .getObservationsByEventType(event.getClass())
        .forEach(definition -> notifySubscription(definition, event, topics));
  }

  @Override
  public <E extends Event, T> void register(
      final Class<T> resultType, final ResultProcessor<E, T> resultProcessor) {
    resultProcessorService.register(resultType, resultProcessor);
  }

  private void notifySubscription(
      final ObserverDefinition definition, final Event event, final String[] topics)
      throws EventPublishingException {
    final Observer observer = definition.observer();
    if (hasSpecifiedTopic(topics) && isExcludedSubscription(observer, topics)) {
      return;
    }

    for (final MethodHandle invocation : definition.invocations()) {
      executor.execute(() -> notifyObservedMethods(invocation, observer, event));
    }
  }

  private void notifyObservedMethods(
      final MethodHandle invocation, final Observer observer, final Event event)
      throws EventPublishingException {
    try {
      final Object returnedValue = invocation.invoke(observer, event);
      if (returnedValue != null && resultProcessorService.isProcessingRequired()) {
        resultProcessorService.tryProcessing(event, returnedValue);
      }
    } catch (final Throwable throwable) {
      throw new EventPublishingException(
          "Could not publish event, because of unexpected throwable during method invocation.",
          throwable);
    }
  }

  private boolean hasSpecifiedTopic(final String[] topics) {
    return topics.length > 0;
  }

  private boolean isExcludedSubscription(final Observer observer, final String[] topics) {
    return stream(topics).noneMatch(topic -> observer.topic().equals(topic));
  }
}
