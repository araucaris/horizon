package io.mikeamiry.aegis.eventbus;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import org.jetbrains.annotations.Nullable;

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
