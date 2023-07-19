package org.dooq.converter.converters;

import org.jetbrains.annotations.Nullable;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class BooleanConverter extends DateConverter {

    protected Boolean parseBoolean(@Nullable AttributeValue value) {
        if (value == null || Boolean.TRUE.equals(value.nul())) return null;

        return value.bool();
    }

    protected boolean parseBool(@Nullable AttributeValue value) {
        if (value == null) return false;

        return Boolean.TRUE.equals(value.bool());
    }

    protected AttributeValue writeBoolean(@Nullable Boolean value) {
        if (value == null) return null;

        return AttributeValue.fromBool(value);
    }

    protected AttributeValue writeBool(boolean value) {
        return AttributeValue.fromBool(value);
    }
}
