package com.neo.springapp.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.List;

@Converter
public class FeatureListConverter implements AttributeConverter<List<String>, String> {
    @Override
    public String convertToDatabaseColumn(List<String> features) {
        return features == null ? null : String.join(",", features);
    }

    @Override
    public List<String> convertToEntityAttribute(String value) {
        return value == null
                ? null
                : value.isBlank()
                ? List.of()
                : Arrays.stream(value.split(",")).map(String::trim).filter(feature -> !feature.isEmpty()).toList();
    }
}
