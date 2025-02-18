package io.mikeamiry.aegis.eventbus;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import org.jetbrains.annotations.Nullable;

/**
 * Provides functionality for managing and processing results that are associated with events in an
 * event-driven system.
 *
 * <p>The {@code ResultProcessorService} class acts as a central registry and processor for handling
 * results produced by event-handling methods. It enables users to register specific {@link
 * ResultProcessor} implementations for different result types, and subsequently handles the
 * processing of these results in a type-aware and context-sensitive manner.
 *
 * <p>Key Responsibilities: - Registration of custom {@link ResultProcessor} implementations for
 * specific result types. - Resolving appropriate result processors based on the class of a result
 * or its assignable type. - Processing of results synchronously or asynchronously if they are
 * wrapped in a {@link CompletionStage}.
 *
 * <p>Thread Safety: - This class is not thread-safe. External synchronization is required if used
 * in a multithreaded context.
 *
 * <p>Functional Description: - Results are processed by resolving the appropriate {@link
 * ResultProcessor} for their type. - Supports asynchronous result processing when results are
 * provided as {@link CompletionStage}.
 *
 * <p>Key Features: - Handles results of varying types seamlessly via type-based registration and
 * lookup. - Ensures processing of both direct results and asynchronous promises.
 *
 * <p>Methods Overview: - {@code register}: Allows the registration of a {@link ResultProcessor} for
 * a given result type. - {@code getResultHandlerByClass}: Resolves a {@link ResultProcessor} for a
 * specific result type. - {@code isProcessingRequired}: Determines if the service has any
 * registered processors. - {@code tryProcessing}: Attempts to process a result associated with a
 * provided event. - {@code processPromise}: Handles the processing of asynchronous results.
 */
final class ResultProcessorService {

  private final Map<Class<?>, ResultProcessor<?, ?>> processors = new HashMap<>();

  <T, E extends Event> void register(
      final Class<T> resultType, final ResultProcessor<E, T> resultProcessor) {
    processors.put(resultType, resultProcessor);
  }

  ResultProcessor<?, ?> getResultHandlerByClass(final Class<?> resultType) {
    final ResultProcessor<?, ?> resultProcessor = processors.get(resultType);
    if (resultProcessor != null) {
      return resultProcessor;
    }

    for (final Map.Entry<Class<?>, ResultProcessor<?, ?>> entry : processors.entrySet()) {
      if (isAssignableFrom(entry.getKey(), resultType)) {
        return entry.getValue();
      }
    }

    return null;
  }

  boolean isProcessingRequired() {
    return !processors.isEmpty();
  }

  private boolean isAssignableFrom(final Class<?> type, final Class<?> otherType) {
    return type.isAssignableFrom(otherType) || otherType.isAssignableFrom(type);
  }

  <E extends Event, T> void tryProcessing(final E event, final @Nullable T value) {
    if (value == null) {
      return;
    }

    final Class<?> resultType = value.getClass();
    if (CompletionStage.class.isAssignableFrom(resultType)) {
      processPromise(event, (CompletionStage<?>) value);
      return;
    }

    final ResultProcessor<?, ?> resultProcessor = getResultHandlerByClass(value.getClass());
    if (resultProcessor == null) {
      throw new ResultProcessingException(
          "Could not handle result of type %s, because of missing result handler."
              .formatted(value.getClass().getName()));
    }

    //noinspection unchecked
    ((ResultProcessor<E, T>) resultProcessor).process(event, value);
  }

  private <E extends Event, T> void processPromise(
      final E event, final CompletionStage<T> resultPromise) {
    resultPromise
        .whenComplete((result, cause) -> tryProcessing(event, result))
        .exceptionally(
            cause -> {
              throw new ResultProcessingException(
                  "Could not handle result of type %s, because of an exception."
                      .formatted(cause.getClass().getName()),
                  cause);
            });
  }
}
