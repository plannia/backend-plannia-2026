package upc.com.pe.backendplannia.shared.infrastructure.persistence.jpa.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import upc.com.pe.backendplannia.shared.domain.model.valueobjects.EmbeddingVector;

import java.util.List;

@Converter
public class EmbeddingVectorConverter implements AttributeConverter<EmbeddingVector, String> {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<Float>> FLOAT_LIST_TYPE = new TypeReference<>() {
    };

    @Override
    public String convertToDatabaseColumn(EmbeddingVector attribute) {
        if (attribute == null) {
            return "[]";
        }

        try {
            return OBJECT_MAPPER.writeValueAsString(attribute.values());
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Could not serialize embedding vector", e);
        }
    }

    @Override
    public EmbeddingVector convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return EmbeddingVector.of(List.of());
        }

        try {
            return EmbeddingVector.of(OBJECT_MAPPER.readValue(dbData, FLOAT_LIST_TYPE));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Could not deserialize embedding vector", e);
        }
    }
}
