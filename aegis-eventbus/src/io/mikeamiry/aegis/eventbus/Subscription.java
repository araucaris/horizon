package io.mikeamiry.aegis.eventbus;

import java.lang.invoke.MethodHandle;
import java.util.Set;

record Subscription(Subscriber subscriber, Set<MethodHandle> invocations) {}
