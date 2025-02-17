package io.mikeamiry.aegis.eventbus;

import java.lang.invoke.MethodHandle;
import java.util.Set;

record ObserverDefinition(Observer observer, Set<MethodHandle> invocations) {}
