package io.mikeamiry.aegis.eventbus;

/**
 * Represents a specific exception that is thrown during the processing of results in the
 * event-driven system.
 *
 * <p>This exception is a specialized form of {@link IllegalStateException}, indicating that an
 * error has occurred while processing results returned by event-handling methods or result
 * processors.
 *
 * <p>Key Use Cases: - Thrown when a failure occurs during result processing in the event-handling
 * workflow. - Serves as a clear signal for issues related to the result processing mechanism within
 * the system. - Typically used internally by components managing result processing, such as the
 * {@link EventBus}.
 *
 * <p>Thread Safety: - Instances of this exception are immutable and can be safely shared across
 * threads.
 *
 * <p>Best Practices: - Catch and handle this exception if custom handling of result processing
 * failures is required. - Investigate the underlying cause to ensure correct resolution of issues
 * in the result processing pipeline.
 */
public final class ResultProcessingException extends IllegalStateException {

  ResultProcessingException(final String message) {
    super(message);
  }

  ResultProcessingException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
