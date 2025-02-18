package io.mikeamiry.aegis.eventbus;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotation to mark a method within an {@link Observer} as an event handler in the event-driven
 * system.
 *
 * <p>Methods annotated with {@code @Observe} are automatically detected and invoked when an event
 * matching the method’s parameter type is published via the {@link EventBus}. These methods serve
 * as entry points for handling events and can optionally return results that may be processed by a
 * registered {@link ResultProcessor}.
 *
 * <p>Key Features: - Designates event-handling methods for automatic invocation. - Supports an
 * event subscription model based on event type and optional topics. - Enables integration with the
 * result processing system for advanced event handling workflows.
 *
 * <p>Constraints: - Annotated methods must belong to an {@link Observer} instance registered with
 * the {@link EventBus}. - The first parameter of the method must be of a type implementing {@link
 * Event}. - The method’s signature and parameters are validated against the event system’s rules.
 *
 * <p>Usage: - Use this annotation on methods that need to handle specific events in the
 * application. - Ensure the containing class implements the {@link Observer} interface and is
 * registered with the event bus.
 *
 * <p>Thread Safety: - The execution of annotated methods depends on the threading model of the
 * {@link java.util.concurrent.Executor} supplied to the {@link EventBusFactory}.
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface Observe {}
