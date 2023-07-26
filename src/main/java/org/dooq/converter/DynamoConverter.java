package org.dooq.converter;

import org.jetbrains.annotations.NotNull;

public interface DynamoConverter {
    static <T> @NotNull Converter<T> getConverter(@NotNull Class<T> type) {
        return ParserCompiler.getConverter(type);
    }

    @SuppressWarnings("rawtypes")
    static <T> @NotNull Converter<T> getConverter(@NotNull Class<T> type, @NotNull Class<? extends ObjectParser> parser) {
        return ParserCompiler.getConverter(type, parser);
    }

}
