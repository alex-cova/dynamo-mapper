package org.dooq.converter.converters;

import org.dooq.converter.ObjectParser;
import org.jetbrains.annotations.Nullable;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.net.URL;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AdditionalConverter extends ConverterHelper {

    protected URL parseURL(@Nullable AttributeValue value) {
        if (value == null || Boolean.TRUE.equals(value.nul())) return null;

        if (value.s() == null) return null;

        try {
            return new URL(value.s());
        } catch (Exception ex) {
            Logger.getLogger(ObjectParser.class.getName())
                    .log(Level.WARNING, "Invalid URL value: '%s'".formatted(value.s()));
        }

        return null;
    }

    protected AttributeValue writeURL(@Nullable URL value) {
        if (value == null) return null;

        return AttributeValue.fromS(value.toString());
    }

    protected UUID parseUUID(@Nullable AttributeValue value) {
        if (value == null || Boolean.TRUE.equals(value.nul())) return null;

        if (value.s() == null) return null;

        try {
            return UUID.fromString(value.s());
        } catch (Exception ex) {
            Logger.getLogger(ObjectParser.class.getName())
                    .log(Level.WARNING, "Invalid UUID value: '%s'".formatted(value.s()));
        }

        return null;
    }

    protected AttributeValue writeUUID(@Nullable UUID value) {
        if (value == null) return null;

        return AttributeValue.fromS(value.toString());
    }
}
