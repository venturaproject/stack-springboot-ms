package com.boilerplate.domain.auth;

import jakarta.annotation.Nullable;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Maps the {@link Email} value object to/from a plain {@code varchar} column.
 * {@code jakarta.*} is the one framework dependency the domain layer is allowed
 * (see ARCHITECTURE.md); Spring is not, so no Spring imports here.
 */
@Converter(autoApply = true)
@SuppressWarnings("NullAway") // JPA converters map null <-> null by contract
public class EmailConverter implements AttributeConverter<Email, String> {

    @Override
    @Nullable
    public String convertToDatabaseColumn(@Nullable Email attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    @Nullable
    public Email convertToEntityAttribute(@Nullable String dbData) {
        return dbData == null || dbData.isBlank() ? null : new Email(dbData);
    }
}
