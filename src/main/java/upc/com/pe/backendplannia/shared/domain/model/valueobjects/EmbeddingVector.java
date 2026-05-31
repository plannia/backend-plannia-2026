package upc.com.pe.backendplannia.shared.domain.model.valueobjects;

import java.util.ArrayList;
import java.util.List;

public record EmbeddingVector(List<Float> values) {

    public static EmbeddingVector of(List<Float> values) {
        return new EmbeddingVector(values);
    }

    public static EmbeddingVector average(List<EmbeddingVector> vectors) {
        if (vectors == null || vectors.isEmpty()) {
            throw new IllegalArgumentException("Vectors must not be null or empty");
        }

        int dim = vectors.getFirst().dimension();
        float[] sum = new float[dim];

        for (EmbeddingVector vector : vectors) {
            if (vector.dimension() != dim) {
                throw new IllegalArgumentException("All vectors must have the same dimension");
            }
            for (int i = 0; i < dim; i++) {
                sum[i] += vector.values().get(i);
            }
        }

        List<Float> average = new ArrayList<>(dim);
        for (float value : sum) {
            average.add(value / vectors.size());
        }

        return new EmbeddingVector(average);
    }

    public int dimension() {
        return values.size();
    }

    public float cosineSimilarity(EmbeddingVector other) {
        if (dimension() != other.dimension()) {
            throw new IllegalArgumentException("Embedding vectors must have the same dimension");
        }
        float dotProduct = 0;
        float magnitudeA = 0;
        float magnitudeB = 0;
        for (int i = 0; i < values.size(); i++) {
            float a = this.values.get(i);
            float b = other.values.get(i);
            dotProduct += a * b;
            magnitudeA += a * a;
            magnitudeB += b * b;
        }
        if (magnitudeA == 0 || magnitudeB == 0) return 0f;
        return dotProduct / (float) Math.sqrt(magnitudeA * magnitudeB);
    }
}
