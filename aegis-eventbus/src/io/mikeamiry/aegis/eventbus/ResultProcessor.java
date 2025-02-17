package io.mikeamiry.aegis.eventbus;

@FunctionalInterface
public interface ResultProcessor<E extends Event, T> {

  void process(final E event, final T result);
}
