package com.boilerplate.domain.api;

import jakarta.annotation.Nullable;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Stores a {@code List<String>} (scopes) as a comma-separated column. Lives in the domain
 * package because it only depends on {@code jakarta.persistence} — ARCHITECTURE.md forbids
 * a {@code domain -> infrastructure} dependency.
 */
@Converter
@SuppressWarnings("NullAway") // JPA converter: tolerates and returns null
public class StringListConverter implements AttributeConverter<List<String>, String> {

    @Override
    public String convertToDatabaseColumn(@Nullable List<String> list) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        return String.join(",", list);
    }

    @Override
    public List<String> convertToEntityAttribute(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(value.split(",")));
    }
}
