package com.hookahmod.integration;

import com.hookahmod.HookahMod;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;

final class KingdomsBridge {
    private final BooleanSupplier loaded;
    private final String className;
    private final Map<String, Optional<Method>> methods = new ConcurrentHashMap<>();
    private final Set<String> reportedFailures = ConcurrentHashMap.newKeySet();

    KingdomsBridge(BooleanSupplier loaded, String className) {
        this.loaded = loaded;
        this.className = className;
    }

    boolean permission(String name, Class<?>[] parameters, Object... args) {
        if (!loaded.getAsBoolean()) return true;
        Object value = invoke(name, parameters, args);
        if (value instanceof Boolean allowed) return allowed;
        if (reportedFailures.add(name)) {
            HookahMod.LOGGER.warn("Kingdoms permission '{}' is unavailable; access denied", name);
        }
        return false;
    }

    @Nullable
    Object invoke(String name, Class<?>[] parameters, Object... args) {
        if (!loaded.getAsBoolean()) return null;
        for (Object arg : args) {
            if (arg == null) return null;
        }
        Method method = methods.computeIfAbsent(name, key -> resolve(key, parameters)).orElse(null);
        if (method == null) return null;
        try {
            return method.invoke(null, args);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            if (reportedFailures.add(name)) {
                HookahMod.LOGGER.warn("Kingdoms integration call '{}' failed; permissions will be denied until it recovers",
                        name, exception);
            }
            return null;
        }
    }

    private Optional<Method> resolve(String name, Class<?>[] parameters) {
        try {
            Class<?> integration = Class.forName(className, false, KingdomsBridge.class.getClassLoader());
            return Optional.of(integration.getMethod(name, parameters));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            if (reportedFailures.add(name)) {
                HookahMod.LOGGER.warn("Kingdoms integration method '{}' is unavailable; permission checks will deny access",
                        name, exception);
            }
            return Optional.empty();
        }
    }
}
