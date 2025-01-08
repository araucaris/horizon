package dev.araucaris.horizon;

import static java.lang.invoke.MethodHandles.lookup;
import static java.lang.invoke.MethodHandles.privateLookupIn;
import static java.lang.reflect.Modifier.isPublic;
import static java.util.Arrays.stream;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableSet;
import static java.util.Map.entry;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toSet;

import dev.araucaris.horizon.packet.Packet;
import dev.araucaris.horizon.packet.PacketException;
import dev.araucaris.horizon.packet.PacketHandler;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

final class SubscriptionService {
  private static final MethodHandles.Lookup LOOKUP = lookup();

  private final Map<SubscriptionCompositeKey, Set<Map.Entry<Object, Set<MethodHandle>>>>
      subscriptionsByPacketType = new ConcurrentHashMap<>();

  static SubscriptionService create() {
    return new SubscriptionService();
  }

  @SuppressWarnings("unchecked")
  public Set<Class<? extends Packet>> subscribe(String topic, Object subscriber)
      throws HorizonException {

    Class<?> subscriberType = subscriber.getClass();
    Map<Class<? extends Packet>, Set<MethodHandle>> methodHandlesByPacketType =
        stream(subscriberType.getDeclaredMethods())
            .filter(
                method ->
                    method.isAnnotationPresent(PacketHandler.class)
                        && Packet.class.isAssignableFrom(method.getParameterTypes()[0]))
            .map(method -> getMethodHandle(subscriberType, method))
            .collect(
                groupingBy(
                    method -> (Class<? extends Packet>) method.type().lastParameterType(),
                    toSet()));

    methodHandlesByPacketType.forEach(
        (packetType, methodHandles) ->
            subscriptionsByPacketType
                .computeIfAbsent(
                    new SubscriptionCompositeKey(packetType, topic), key -> new HashSet<>())
                .add(entry(subscriber, methodHandles)));

    return methodHandlesByPacketType.keySet();
  }

  public Set<Map.Entry<Object, Set<MethodHandle>>> retrieveByPacketTypeAndTopic(
      Class<? extends Packet> type, String topic) {
    return unmodifiableSet(
        subscriptionsByPacketType.getOrDefault(
            new SubscriptionCompositeKey(type, topic), emptySet()));
  }

  private MethodHandle getMethodHandle(Class<?> type, Method method) {
    try {
      MethodHandles.Lookup lookup =
          isPublic(type.getModifiers()) ? LOOKUP : privateLookupIn(type, LOOKUP);
      return lookup.unreflect(method);
    } catch (Exception exception) {
      throw new PacketException(
          "Could not get method handle for %s method, at %s.".formatted(method.getName(), type),
          exception);
    }
  }

  record SubscriptionCompositeKey(Class<? extends Packet> packetType, String topic) {}
}
