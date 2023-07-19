package org.dooq.converter;

import org.dooq.converter.converters.CollectionConverter;
import org.jetbrains.annotations.Nullable;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Function;
import java.util.logging.Logger;

@SuppressWarnings("unused")
public abstract class ObjectParser<T> extends CollectionConverter implements Converter<T> {

    @Override
    protected Function<AttributeValue, ?> lookUpParser(Class<?> type) {

        if (type == String.class) {
            return this::parseString;
        }

        if (type == Integer.class) {
            return this::parseInteger;
        }

        if (type == BigDecimal.class) {
            return this::parseBigDecimal;
        }

        if (type == Boolean.class) {
            return this::parseBoolean;
        }

        if (type == Long.class) {
            return this::parseLong;
        }

        if (type == Float.class) {
            return this::parseFloat;
        }

        if (type == LocalDate.class) {
            return this::parseLocalDate;
        }

        if (type == LocalDateTime.class) {
            return this::parseLocalDateTime;
        }

        if (type == UUID.class) {
            return this::parseUUID;
        }

        Logger.getLogger("ObjectParser").warning("Not found parser for value: " + type.getName());

        return null;
    }

    @Override
    protected AttributeValue lookUp(@Nullable Object value) {

        if (value == null) return null;

        if (value instanceof String string) {
            return writeString(string);
        }

        if (value instanceof UUID uuid) {
            return writeUUID(uuid);
        }

        if (value instanceof Long l) {
            return writeLong(l);
        }

        if (value instanceof Boolean bool) {
            return writeBoolean(bool);
        }

        if (value instanceof Integer i) {
            return writeInteger(i);
        }

        if (value instanceof Float f) {
            return writeFloat(f);
        }

        if (value instanceof BigDecimal bd) {
            return writeBigDecimal(bd);
        }


        System.err.println("Not found: " + value.getClass());

        return null;
    }


    protected <K> AttributeValue writeComplex(@Nullable K value, Class<K> type) {

        if (value == null) return null;

        ObjectParser<K> parser = ParserCompiler.getConverter(type);

        return AttributeValue.fromM(parser.write(value));
    }


    @Override
    protected <V> V parseComplex(@Nullable AttributeValue value, Class<V> type) {

        if (value == null || Boolean.TRUE.equals(value.nul())) return null;

        if (isJVMClass(type)) {
            return lookUp(value, type);
        }

        if (value.m() != null) {
            var parser = ParserCompiler.getConverter(type);

            return parser.read(value.m());
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    protected <V> V lookUp(AttributeValue value, Class<V> type) {
        if (type == String.class) {
            return (V) parseString(value);
        }

        if (type == Integer.class) {
            return (V) parseInteger(value);
        }

        if (type == BigDecimal.class) {
            return (V) parseBigDecimal(value);
        }

        if (type == UUID.class) {
            return (V) parseUUID(value);
        }

        if (type == Boolean.class) {
            return (V) parseBoolean(value);
        }

        if (type == LocalDate.class) {
            return (V) parseLocalDate(value);
        }

        if (type == LocalDateTime.class) {
            return (V) parseLocalDateTime(value);
        }

        throw new IllegalStateException("Value not implemented: " + type);
    }


}
