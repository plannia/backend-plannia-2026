package upc.com.pe.backendplannia.profile.infrastructure.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.stereotype.Service;
import upc.com.pe.backendplannia.profile.application.internal.outboundservices.ai.ProfileEmbeddingService;
import upc.com.pe.backendplannia.shared.domain.model.valueobjects.EmbeddingVector;

import java.util.ArrayList;
import java.util.List;

@Service
public class OpenAiProfileEmbeddingService implements ProfileEmbeddingService {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenAiProfileEmbeddingService.class);

    private final EmbeddingModel embeddingModel;

    public OpenAiProfileEmbeddingService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    @Override
    public EmbeddingVector generateEmbedding(String text) {
        int textLength = text == null ? 0 : text.length();
        LOGGER.info("Generating profile/task embedding: textLength={}", textLength);
        try {
            var request = new EmbeddingRequest(List.of(text), null);
            var response = embeddingModel.call(request);
            float[] vector = response.getResults().getFirst().getOutput();
            LOGGER.info("Embedding generated: textLength={}, embeddingDim={}", textLength, vector.length);
            return toVector(vector);
        } catch (RuntimeException exception) {
            LOGGER.error("Embedding generation failed: textLength={}", textLength, exception);
            throw exception;
        }
    }

    @Override
    public List<EmbeddingVector> generateEmbeddings(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }
        LOGGER.info("Generating {} profile item embeddings in one call", texts.size());
        try {
            var response = embeddingModel.call(new EmbeddingRequest(texts, null));
            return response.getResults().stream()
                    .map(result -> toVector(result.getOutput()))
                    .toList();
        } catch (RuntimeException exception) {
            LOGGER.error("Batch embedding generation failed: count={}", texts.size(), exception);
            throw exception;
        }
    }

    private EmbeddingVector toVector(float[] vector) {
        List<Float> floatList = new ArrayList<>(vector.length);
        for (float f : vector) floatList.add(f);
        return EmbeddingVector.of(floatList);
    }
}
