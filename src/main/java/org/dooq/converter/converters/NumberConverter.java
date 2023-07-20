package org.dooq.converter.converters;

import org.jetbrains.annotations.Nullable;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.math.BigDecimal;

public class NumberConverter extends StringConverter {

    protected AttributeValue writeInt(int value) {
        return AttributeValue.fromN(String.valueOf(value));
    }

    protected AttributeValue writeShort(short value) {
        return AttributeValue.fromN(String.valueOf(value));
    }

    protected AttributeValue writeFloat(float value) {
        return AttributeValue.fromN(String.valueOf(value));
    }

    protected AttributeValue writeLong(long value) {
        return AttributeValue.fromN(String.valueOf(value));
    }

    protected AttributeValue writeInteger(@Nullable Integer value) {
        if (value == null) return null;

        return AttributeValue.fromN(String.valueOf(value));
    }

    protected AttributeValue writeShorter(@Nullable Short value) {
        if (value == null) return null;

        return AttributeValue.fromN(String.valueOf(value));
    }

    protected AttributeValue writeLonger(@Nullable Long value) {
        if (value == null) return null;

        return AttributeValue.fromN(String.valueOf(value));
    }

    protected AttributeValue writeFloater(@Nullable Float value) {
        if (value == null) return null;

        return AttributeValue.fromN(String.valueOf(value));
    }

    protected AttributeValue writeBigDecimal(@Nullable BigDecimal value) {
        if (value == null) return null;

        return AttributeValue.fromN(value.toPlainString());
    }

    protected int parseInt(@Nullable AttributeValue value) {
        if (value == null) return 0;
        return Integer.parseInt(value.n());
    }

    protected int parseShort(@Nullable AttributeValue value) {
        if (value == null) return 0;
        return Short.parseShort(value.n());
    }

    protected Integer parseInteger(@Nullable AttributeValue value) {
        if (value == null || Boolean.TRUE.equals(value.nul())) return null;

        if (value.n() == null) return null;

        return Integer.valueOf(value.n());
    }

    protected Short parseShorter(@Nullable AttributeValue value) {
        if (value == null || Boolean.TRUE.equals(value.nul())) return null;

        if (value.n() == null) return null;

        return Short.valueOf(value.n());
    }

    protected Long parseLonger(@Nullable AttributeValue value) {

        if (value == null || Boolean.TRUE.equals(value.nul())) return null;

        if (value.n() == null) return null;

        return Long.valueOf(value.n());
    }

    protected Float parseFloater(@Nullable AttributeValue value) {
        if (value == null || Boolean.TRUE.equals(value.nul())) return null;

        return Float.valueOf(value.n());
    }

    protected float parseFloat(@Nullable AttributeValue value) {
        if (value == null || Boolean.TRUE.equals(value.nul())) return 0.0f;

        return Float.parseFloat(value.n());
    }

    protected long parseLong(@Nullable AttributeValue value) {
        if (value == null) return 0;
        if (value.n() == null) return 0;

        return Long.parseLong(value.n());
    }

    protected BigDecimal parseBigDecimal(@Nullable AttributeValue value) {
        if (value == null || Boolean.TRUE.equals(value.nul())) return null;

        if (value.n() != null) {
            return new BigDecimal(value.n());
        }

        if (value.s() != null) {
            return new BigDecimal(value.s());
        }

        return null;
    }
}
