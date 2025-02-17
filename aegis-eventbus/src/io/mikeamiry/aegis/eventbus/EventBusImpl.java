package io.mikeamiry.aegis.eventbus;

import static java.util.Arrays.stream;

import java.lang.invoke.MethodHandle;

final class EventBusImpl implements EventBus {

  private final ObservationService observationService;
  private final ResultProcessorService resultProcessorService;

  EventBusImpl(
      final ObservationService observationService,
      final ResultProcessorService resultProcessorService) {
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

    definition
        .invocations()
        .forEach(invocation -> notifyObservedMethods(invocation, observer, event));
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
