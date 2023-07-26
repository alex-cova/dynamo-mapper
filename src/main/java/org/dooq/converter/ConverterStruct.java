package org.dooq.converter;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds the introspected information about a converter class.
 * <p>
 * Each converter class is introspected only once and the information is cached separately
 * to prevent incorrect behavior when multiple converters are used.
 *
 * @author alex
 */
class ConverterStruct {

    private final Map<Class<?>, ObjectParser<?>> cache = new ConcurrentHashMap<>();
    private final Map<Class<?>, Method> writerMap = new HashMap<>();
    private final Map<Class<?>, Method> readerMap = new HashMap<>();
    private final Map<String, Method> methodMap = new HashMap<>();
    private final Class<?> converter;

    ConverterStruct(@NotNull Class<?> converter) {
        this.converter = converter;

        if (!Modifier.isAbstract(converter.getModifiers()))
            throw new IllegalArgumentException("class converter '%s' must be an abstract class"
                    .formatted(converter.getName()));

        introspect(converter);
    }

    private void introspect(@NotNull Class<?> converter) {
        var methods = converter.getDeclaredMethods();

        for (Method method : methods) {

            if (method.isSynthetic()) continue;

            if (!Modifier.isProtected(method.getModifiers())) continue;

            methodMap.put(method.getName(), method);

            if (method.getParameterCount() != 1) continue;

            var parameter = method.getParameterTypes()[0];

            if (parameter == AttributeValue.class) {
                readerMap.put(method.getReturnType(), method);
            } else {

                if (parameter == Map.class) continue;
                if (parameter == List.class) continue;
                if (parameter == Set.class) continue;

                writerMap.put(parameter, method);
            }
        }

        if (converter.getSuperclass() != Object.class) {
            introspect(converter.getSuperclass());
        }

    }

    public Method getWriter(Class<?> type) {
        return writerMap.get(type);
    }

    public Method getReader(Class<?> type) {
        return readerMap.get(type);
    }

    public Method getMethod(String name) {
        return methodMap.get(name);
    }

    public Class<?> getConverter() {
        return converter;
    }

    public @Nullable ObjectParser<?> getCachedParser(Class<?> target) {
        return cache.get(target);
    }

    public <T> void putCachedParser(Class<T> type, ObjectParser<T> parserInstance) {
        cache.put(type, parserInstance);
    }
}



