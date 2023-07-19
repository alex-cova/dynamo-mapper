package org.dooq.converter.converters;

import org.dooq.converter.Converter;
import org.dooq.converter.DynamoConverter;
import org.jetbrains.annotations.Nullable;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class CollectionConverter extends NumberConverter {

    protected abstract AttributeValue lookUp(@Nullable Object value);

    protected abstract Function<AttributeValue, ?> lookUpParser(Class<?> type);

    protected abstract <V> V parseComplex(@Nullable AttributeValue value, Class<V> type);

    protected AttributeValue writeStringMap(@Nullable Map<String, String> value) {
        if (value == null) return null;

        return AttributeValue.fromM(value.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, b -> AttributeValue.fromS(b.getValue()))));
    }

    @SuppressWarnings("unchecked")
    protected AttributeValue writeMap(@Nullable Map<String, ?> value, Class<?> type) {
        if (value == null) return null;

        if (type == String.class) {
            return writeStringMap((Map<String, String>) value);
        }

        return AttributeValue.fromM(value.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, b -> lookUp(b.getValue()))));
    }

    protected <K> AttributeValue writeList(@Nullable List<K> value, Class<K> type) {

        if (value == null) return null;

        if (value.isEmpty()) return null;

        if (isComplex(type)) {

            Converter<K> parser = DynamoConverter.getConverter(type);

            return AttributeValue.fromL(value.stream()
                    .map(a -> AttributeValue.fromM(parser.write(a)))
                    .filter(Objects::nonNull)
                    .toList());
        }

        return AttributeValue.fromL(value.stream()
                .map(this::lookUp)
                .filter(Objects::nonNull)
                .toList());
    }

    protected Set<?> parseSet(AttributeValue value, Class<?> type) {

        if (value == null || Boolean.TRUE.equals(value.nul())) return null;

        if (value.ss() == null) return null;

        if (type == String.class) {
            return parseStringSet(value);
        }

        if (type == BigDecimal.class) {
            return value.ss().stream()
                    .map(BigDecimal::new)
                    .collect(Collectors.toSet());
        }

        return null;
    }

    protected List<?> parseList(AttributeValue value, Class<?> type) {

        if (value == null || Boolean.TRUE.equals(value.nul())) return null;

        @Nullable var converter = lookUpParser(type);

        if (converter != null) {
            return value.l().stream()
                    .map(converter)
                    .filter(Objects::nonNull)
                    .toList();
        }

        if (isComplex(type)) {
            return value.l().stream()
                    .map(v -> parseComplex(v, type))
                    .filter(Objects::nonNull)
                    .toList();
        }

        return null;
    }

    protected Map<String, ?> parseMap(@Nullable AttributeValue value, Class<?> type) {

        if (value == null || Boolean.TRUE.equals(value.nul())) return null;

        if (value.m() == null) return null;

        if (isComplex(type)) {

            return value.m().entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, v -> parseComplex(v.getValue(), type)));
        }

        Map<String, Object> resultMap = new HashMap<>(value.m().size());

        Function<AttributeValue, ?> parser = lookUpParser(type);

        for (Map.Entry<String, AttributeValue> entry : value.m().entrySet()) {
            resultMap.put(entry.getKey(), parser.apply(entry.getValue()));
        }

        return resultMap;

    }

    protected List<String> parseStringList(@Nullable AttributeValue value) {
        if (value == null || Boolean.TRUE.equals(value.nul())) return null;

        if (value.hasL()) {
            return value.l()
                    .stream()
                    .map(this::parseString)
                    .toList();
        }

        if (value.hasSs()) {
            return value.ss();
        }

        return null;
    }

    protected Set<String> parseStringSet(@Nullable AttributeValue value) {

        if (value == null || Boolean.TRUE.equals(value.nul())) return null;

        if (value.ss() == null) return null;

        return new HashSet<>(value.ss());
    }

    protected AttributeValue writeSet(@Nullable Set<?> value, Class<?> type) {

        if (value == null) return null;

        return AttributeValue.fromSs(value.stream()
                .map(Object::toString)
                .toList());
    }

    protected AttributeValue writeStringSet(@Nullable Set<String> value) {
        if (value == null) return null;

        return AttributeValue.fromSs(new ArrayList<>(value));
    }

    protected AttributeValue writeStringList(@Nullable List<String> value) {
        if (value == null) return null;

        return AttributeValue.fromL(value.stream()
                .map(AttributeValue::fromS)
                .toList());
    }
}
