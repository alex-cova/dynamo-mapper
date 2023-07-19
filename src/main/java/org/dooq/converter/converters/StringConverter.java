package org.dooq.converter.converters;

import org.jetbrains.annotations.Nullable;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class StringConverter extends BooleanConverter {

    protected AttributeValue writeString(@Nullable String value) {
        if (value == null) return null;

        return AttributeValue.fromS(value);
    }

    protected String parseString(@Nullable AttributeValue value) {

        if (value == null || Boolean.TRUE.equals(value.nul())) return null;

        return value.s();
    }
}
