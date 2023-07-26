package org.dooq.converter;

import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.List;
import java.util.Map;

public interface Converter<T> {

    @NotNull T read(@NotNull Map<String, AttributeValue> value);

    @NotNull T newInstance();

    @NotNull Map<String, AttributeValue> write(@NotNull T value);

    default List<T> readAll(@NotNull List<Map<String, AttributeValue>> value) {
        return value.stream()
                .map(this::read)
                .toList();
    }

    default List<Map<String, AttributeValue>> writeAll(@NotNull List<T> list) {
        return list.stream()
                .map(this::write)
                .toList();
    }
}
