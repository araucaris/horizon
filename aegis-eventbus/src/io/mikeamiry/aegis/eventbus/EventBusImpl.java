package io.mikeamiry.aegis.eventbus;

import static java.util.Arrays.stream;

import java.lang.invoke.MethodHandle;

final class EventBusImpl implements EventBus {

  private final SubscriptionService subscriptionService;
  private final ResultProcessorService resultProcessorService;

  EventBusImpl(
      final SubscriptionService subscriptionService,
      final ResultProcessorService resultProcessorService) {
    this.subscriptionService = subscriptionService;
    this.resultProcessorService = resultProcessorService;
  }

  @Override
  public void subscribe(final Subscriber subscriber) throws SubscribingException {
    subscriptionService.subscribe(subscriber);
  }

  @Override
  public void publish(final Event event, final String... topics) throws EventPublishingException {
    subscriptionService
        .getSubscriptionsByEventType(event.getClass())
        .forEach(subscription -> notifySubscription(subscription, event, topics));
  }

  @Override
  public <E extends Event, T> void register(
      final Class<T> resultType, final ResultProcessor<E, T> resultProcessor) {
    resultProcessorService.register(resultType, resultProcessor);
  }

  private void notifySubscription(
      final Subscription subscription, final Event event, final String[] topics)
      throws EventPublishingException {
    final Subscriber subscriber = subscription.subscriber();
    if (hasSpecifiedTopic(topics) && isExcludedSubscription(subscriber, topics)) {
      return;
    }

    subscription
        .invocations()
        .forEach(invocation -> notifySubscribedMethods(invocation, subscriber, event));
  }

  private void notifySubscribedMethods(
      final MethodHandle invocation, final Subscriber subscriber, final Event event)
      throws EventPublishingException {
    try {
      final Object returnedValue = invocation.invoke(subscriber, event);
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

  private boolean isExcludedSubscription(final Subscriber subscriber, final String[] topics) {
    return stream(topics).noneMatch(topic -> subscriber.topic().equals(topic));
  }
}
