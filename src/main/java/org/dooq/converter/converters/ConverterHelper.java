package org.dooq.converter.converters;

import org.jetbrains.annotations.NotNull;

public class ConverterHelper {

    protected boolean isComplex(@NotNull Class<?> type) {
        return !type.getName().startsWith("java");
    }

    protected boolean isJVMClass(@NotNull Class<?> type) {
        return type.getName().startsWith("java");
    }
}
