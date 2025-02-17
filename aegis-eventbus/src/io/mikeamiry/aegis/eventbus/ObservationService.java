package io.mikeamiry.aegis.eventbus;

import static java.lang.invoke.MethodHandles.lookup;
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

final class ObservationService {

  private static final MethodHandles.Lookup LOOKUP = lookup();
  private final Map<Class<? extends Event>, Set<ObserverDefinition>> observationsByEventType;

  ObservationService() {
    this.observationsByEventType = new HashMap<>();
  }

  private static MethodHandle getMethodHandle(final Class<?> type, final Method method) {
    try {
      return getLookupForClass(type).unreflect(method);
    } catch (final IllegalAccessException exception) {
      throw new ObservingException(
          "Could not resolve method handle for %s method, because of illegal access."
              .formatted(method.getName()),
          exception);
    }
  }

  private static MethodHandles.Lookup getLookupForClass(final Class<?> clazz)
      throws IllegalAccessException {
    return isPublic(clazz.getModifiers()) ? LOOKUP : privateLookupIn(clazz, LOOKUP);
  }

  void observe(final Observer observer) throws ObservingException {
    final Class<? extends Observer> observerType = observer.getClass();
    stream(observerType.getDeclaredMethods())
        .filter(this::isEligibleForObservation)
        .map(method -> getMethodHandle(observerType, method))
        .collect(groupingBy(this::extractEventClass, toSet()))
        .forEach(
            (key, value) ->
                observationsByEventType
                    .computeIfAbsent(key, k -> new HashSet<>())
                    .add(new ObserverDefinition(observer, value)));
  }

  Set<ObserverDefinition> getObservationsByEventType(final Class<? extends Event> eventType) {
    return unmodifiableSet(observationsByEventType.getOrDefault(eventType, emptySet()));
  }

  private boolean isEligibleForObservation(final Method method) {
    return method.isAnnotationPresent(Observe.class)
        && method.getParameterCount() == 1
        && Event.class.isAssignableFrom(method.getParameterTypes()[0]);
  }

  @SuppressWarnings("unchecked")
  private Class<? extends Event> extractEventClass(final MethodHandle method) {
    return (Class<? extends Event>) method.type().lastParameterType();
  }
}
