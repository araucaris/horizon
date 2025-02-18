package io.mikeamiry.aegis.eventbus;

/**
 * Represents a marker interface for events in an event-driven system.
 *
 * <p>Classes implementing this interface signify that they can act as events, typically used with
 * event buses or related mechanisms to facilitate publish/subscribe communication patterns.
 *
 * <p>This interface does not define any methods, serving as a common type for objects that can be
 * observed, processed, or passed through the event-handling framework.
 */
public interface Event {}
