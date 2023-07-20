package org.dooq.converter;

import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;

public interface Converter<T> {

    @NotNull T read(@NotNull Map<String, AttributeValue> value);

    @NotNull T newInstance();

    @NotNull Map<String, AttributeValue> write(@NotNull T value);
}
