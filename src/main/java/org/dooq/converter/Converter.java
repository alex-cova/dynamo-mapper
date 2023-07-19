package org.dooq.converter;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;

public interface Converter<T> {

    T read(Map<String, AttributeValue> value);

    T newInstance();

    Map<String, AttributeValue> write(T value);
}
