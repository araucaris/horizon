package io.mikeamiry.aegis.eventbus;

import java.lang.invoke.MethodHandle;
import java.util.Set;

/**
 * Represents the definition of an observer within the event-driven system.
 *
 * <p>This record encapsulates the association between an {@link Observer} instance and the set of
 * method handles representing the event-handling methods that are invoked for specific events.
 *
 * <p>Key Details: - The {@link Observer} is the entity subscribed to specific events, and its
 * annotated methods handle events as they are published. - {@link MethodHandle} objects represent
 * the specific methods within the observer that are annotated with {@link Observe} and are eligible
 * for invocation when matching events are published.
 *
 * <p>Purpose: - Facilitates the organization and invocation of event-handling logic associated with
 * an observer. - Serves as a structural definition used internally by the event system to match
 * events with their respective observers' methods.
 *
 * <p>Thread Safety: - Thread safety for the observer methods depends on their implementation and
 * the threading model used by the {@link EventBus}.
 */
record ObserverDefinition(Observer observer, Set<MethodHandle> invocations) {}
