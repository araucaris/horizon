package io.mikeamiry.aegis.eventbus;

/**
 * Represents an exception that is thrown when observing an {@link Observer} in the event-driven
 * system encounters an error.
 *
 * <p>This exception typically arises during the registration or invocation of methods annotated
 * with {@link Observe}, when something unexpected happens during the process of setting up or
 * communicating with event observers.
 *
 * <p>Common Scenarios: - Encountering an invalid observer configuration during registration with
 * the {@link EventBus}. - Exceptions triggered by malfunctioning observers or parameters during
 * event observation.
 *
 * <p>Thread Safety: - As with any exception, instances of this class are immutable and therefore
 * thread-safe.
 *
 * <p>When this exception is thrown, it carries a detailed message and a root cause that can help
 * diagnose the underlying issue.
 */
public final class ObservingException extends RuntimeException {

  ObservingException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
