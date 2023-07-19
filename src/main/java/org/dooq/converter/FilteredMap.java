package org.dooq.converter;

import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public final class FilteredMap extends HashMap<String, AttributeValue> {

    public FilteredMap() {
    }

    @Override
    public AttributeValue put(String key, AttributeValue value) {
        if (value == null) return null;

        return super.put(key, value);
    }

    @Override
    public AttributeValue putIfAbsent(String key, AttributeValue value) {
        if (value == null) return null;

        return super.putIfAbsent(key, value);
    }

    @Override
    public void putAll(@NotNull Map<? extends String, ? extends AttributeValue> m) {

        var resultMap = m.entrySet().stream()
                .filter(a -> a.getValue() != null)
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

        super.putAll(resultMap);
    }
}
