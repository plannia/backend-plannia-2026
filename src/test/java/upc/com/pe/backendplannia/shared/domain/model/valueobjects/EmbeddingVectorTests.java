package upc.com.pe.backendplannia.shared.domain.model.valueobjects;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.data.Offset.offset;

class EmbeddingVectorTests {

    @Test
    void dimensionReflectsNumberOfValues() {
        assertThat(EmbeddingVector.of(List.of(1f, 2f, 3f)).dimension()).isEqualTo(3);
        assertThat(EmbeddingVector.of(List.of()).dimension()).isZero();
    }

    @Test
    void averageComputesElementWiseMean() {
        var average = EmbeddingVector.average(List.of(
                EmbeddingVector.of(List.of(2f, 4f)),
                EmbeddingVector.of(List.of(4f, 8f))
        ));

        assertThat(average.values()).containsExactly(3f, 6f);
    }

    @Test
    void averageRejectsEmptyInput() {
        assertThatThrownBy(() -> EmbeddingVector.average(List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Vectors must not be null or empty");
    }

    @Test
    void averageRejectsNullInput() {
        assertThatThrownBy(() -> EmbeddingVector.average(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Vectors must not be null or empty");
    }

    @Test
    void averageRejectsMismatchedDimensions() {
        assertThatThrownBy(() -> EmbeddingVector.average(List.of(
                EmbeddingVector.of(List.of(1f, 2f)),
                EmbeddingVector.of(List.of(1f, 2f, 3f))
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("All vectors must have the same dimension");
    }

    @Test
    void cosineSimilarityOfIdenticalVectorsIsOne() {
        var vector = EmbeddingVector.of(List.of(1f, 2f, 3f));

        assertThat(vector.cosineSimilarity(vector)).isCloseTo(1f, offset(0.0001f));
    }

    @Test
    void cosineSimilarityOfOrthogonalVectorsIsZero() {
        var a = EmbeddingVector.of(List.of(1f, 0f));
        var b = EmbeddingVector.of(List.of(0f, 1f));

        assertThat(a.cosineSimilarity(b)).isEqualTo(0f);
    }

    @Test
    void cosineSimilarityReturnsZeroWhenAVectorHasZeroMagnitude() {
        var zero = EmbeddingVector.of(List.of(0f, 0f));
        var nonZero = EmbeddingVector.of(List.of(1f, 1f));

        assertThat(zero.cosineSimilarity(nonZero)).isEqualTo(0f);
    }

    @Test
    void cosineSimilarityRejectsMismatchedDimensions() {
        var a = EmbeddingVector.of(List.of(1f, 2f));
        var b = EmbeddingVector.of(List.of(1f, 2f, 3f));

        assertThatThrownBy(() -> a.cosineSimilarity(b))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Embedding vectors must have the same dimension");
    }
}
