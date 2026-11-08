package com.boilerplate.domain.auth;

import jakarta.annotation.Nullable;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
@SuppressWarnings("NullAway") // JPA converters map null <-> null by contract
public class HashedPasswordConverter implements AttributeConverter<HashedPassword, String> {

    @Override
    @Nullable
    public String convertToDatabaseColumn(@Nullable HashedPassword attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    @Nullable
    public HashedPassword convertToEntityAttribute(@Nullable String dbData) {
        return dbData == null || dbData.isBlank() ? null : new HashedPassword(dbData);
    }
}
