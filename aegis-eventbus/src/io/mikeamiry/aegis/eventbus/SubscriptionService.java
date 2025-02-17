package io.mikeamiry.aegis.eventbus;

import static java.lang.invoke.MethodHandles.privateLookupIn;
import static java.lang.reflect.Modifier.isPublic;
import static java.util.Arrays.stream;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableSet;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toSet;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

final class SubscriptionService {

  private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
  private final Map<Class<? extends Event>, Set<Subscription>> subscriptionsByEventType =
      new HashMap<>();

  SubscriptionService() {}

  void subscribe(final Subscriber subscriber) throws SubscribingException {
    final Class<? extends Subscriber> subscriberType = subscriber.getClass();
    final Map<Class<? extends Event>, Set<MethodHandle>> methodReferencesByEventType =
        stream(subscriberType.getDeclaredMethods())
            .filter(this::isSubscribedMethod)
            .map(method -> getMethodHandle(subscriberType, method))
            .collect(groupingBy(this::getSubscribedEvent, toSet()));
    for (final Map.Entry<Class<? extends Event>, Set<MethodHandle>> entry :
        methodReferencesByEventType.entrySet()) {
      subscriptionsByEventType
          .computeIfAbsent(entry.getKey(), key -> new HashSet<>())
          .add(new Subscription(subscriber, entry.getValue()));
    }
  }

  Set<Subscription> getSubscriptionsByEventType(final Class<? extends Event> eventType) {
    return unmodifiableSet(subscriptionsByEventType.getOrDefault(eventType, emptySet()));
  }

  private boolean isSubscribedMethod(final Method method) {
    return method.isAnnotationPresent(Subscribe.class)
        && method.getParameterCount() == 1
        && Event.class.isAssignableFrom(method.getParameterTypes()[0]);
  }

  @SuppressWarnings("unchecked")
  private Class<? extends Event> getSubscribedEvent(final MethodHandle method) {
    return (Class<? extends Event>) method.type().lastParameterType();
  }

  private MethodHandle getMethodHandle(final Class<?> subscriberType, final Method method) {
    try {
      return getLookupForClass(subscriberType).unreflect(method);
    } catch (final IllegalAccessException exception) {
      throw new SubscribingException(
          "Could not resolve method handle for %s method, because of illegal access."
              .formatted(method.getName()),
          exception);
    }
  }

  private MethodHandles.Lookup getLookupForClass(final Class<?> clazz)
      throws IllegalAccessException {
    return isPublic(clazz.getModifiers()) ? LOOKUP : privateLookupIn(clazz, LOOKUP);
  }
}
