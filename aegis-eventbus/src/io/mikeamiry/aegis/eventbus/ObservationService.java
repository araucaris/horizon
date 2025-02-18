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

/**
 * The ObservationService class manages the registration and mapping of observer methods to their
 * corresponding event types. It plays a core role in the event-driven architecture by overseeing
 * how observer methods are mapped and invoked when events occur.
 *
 * <p>This service: - Registers observer instances and maps their eligible methods annotated with
 * {@code @Observe} to event types these methods observe. - Stores these observer definitions
 * grouped by event types. - Provides access to observer definitions by event type for
 * event-publishing mechanisms.
 *
 * <p>Observers are required to comply with the following constraints for their methods to be
 * eligible: - Methods must have the {@code @Observe} annotation. - Methods must declare exactly one
 * parameter, which must be assignable from the {@code Event} type.
 *
 * <p>Objects of this class are immutable once created and are not designed for inheritance or
 * modification.
 *
 * <p>This class relies on: - Java's {@link MethodHandle} for efficient method invocation. - {@code
 * java.util.Map} and {@code java.util.Set} to store and organize registered observers.
 *
 * <p>Exceptions: - Throws {@code ObservingException} when method resolution or registration fails
 * due to accessibility issues or invalid observer definitions.
 */
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
