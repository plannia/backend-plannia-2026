package upc.com.pe.backendplannia.shared.infrastructure.persistence.jpa.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import upc.com.pe.backendplannia.shared.domain.model.valueobjects.EmbeddingVector;

import java.util.List;

/**
 * Persiste una lista de embeddings (uno por ítem: por habilidad, por interés, por tarea completada)
 * como JSON de lista-de-listas. Habilita el scoring "máximo por ítem" sin promediar.
 */
@Converter
public class EmbeddingVectorListConverter implements AttributeConverter<List<EmbeddingVector>, String> {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<List<Float>>> TYPE = new TypeReference<>() {
    };

    @Override
    public String convertToDatabaseColumn(List<EmbeddingVector> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "[]";
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(attribute.stream().map(EmbeddingVector::values).toList());
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Could not serialize embedding vector list", e);
        }
    }

    @Override
    public List<EmbeddingVector> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return List.of();
        }
        try {
            return OBJECT_MAPPER.<List<List<Float>>>readValue(dbData, TYPE).stream()
                    .map(EmbeddingVector::of)
                    .toList();
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Could not deserialize embedding vector list", e);
        }
    }
}
