package io.mikeamiry.aegis;

/**
 * AegisException represents a specific exception type for issues encountered within the Aegis
 * framework.
 *
 * <p>This exception is a subclass of IllegalStateException and is typically used when the state of
 * Aegis-related components deviates unexpectedly, often as a result of resource management or
 * operational failures, such as during cleanup operations or connection handling.
 *
 * <p>Constructors: - Accepts a detailed message and a cause, providing contextual information about
 * the error and its originating exception.
 *
 * <p>Use cases for this exception commonly include errors encountered while managing or closing
 * resources such as Redis connections or other internal components within the Aegis framework.
 */
public final class AegisException extends IllegalStateException {

  AegisException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
