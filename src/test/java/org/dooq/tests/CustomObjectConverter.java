package org.dooq.tests;

import org.dooq.converter.ObjectParser;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public abstract class CustomObjectConverter<T> extends ObjectParser<T> {

    @Override
    protected AttributeValue writeString(String value) {
        return AttributeValue.fromS("custom");
    }
}
