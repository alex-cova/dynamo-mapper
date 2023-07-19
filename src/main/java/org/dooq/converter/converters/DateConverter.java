package org.dooq.converter.converters;

import org.jetbrains.annotations.Nullable;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;

public class DateConverter extends AdditionalConverter {

    protected AttributeValue writeLocalDate(@Nullable LocalDate value) {
        if (value == null) return null;

        return AttributeValue.fromS(value.toString());
    }

    protected AttributeValue writeLocalTime(@Nullable LocalTime value) {
        if (value == null) return null;

        return AttributeValue.fromS(value.toString());
    }

    protected AttributeValue writeLocalDateTime(@Nullable LocalDateTime value) {
        if (value == null) return null;

        return AttributeValue.fromS(value.toString());
    }


    protected LocalDate parseLocalDate(@Nullable AttributeValue value) {
        if (value == null || Boolean.TRUE.equals(value.nul())) return null;

        if (value.s() != null) {
            return LocalDate.parse(value.s());
        }

        if (value.n() != null) {
            return LocalDate.ofEpochDay(Long.parseLong(value.n()));
        }

        return null;
    }

    protected LocalDateTime parseLocalDateTime(@Nullable AttributeValue value) {
        if (value == null || Boolean.TRUE.equals(value.nul())) return null;

        if (value.s() != null) {
            return LocalDateTime.parse(value.s());
        }

        if (value.n() != null) {
            return LocalDateTime.ofEpochSecond(Long.parseLong(value.n()), 0, ZoneOffset.UTC);
        }

        return null;
    }
}
