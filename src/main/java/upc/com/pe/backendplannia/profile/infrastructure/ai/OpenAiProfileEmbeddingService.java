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

            List<Float> floatList = new ArrayList<>();
            for (float f : vector) floatList.add(f);
            LOGGER.info("Embedding generated: textLength={}, embeddingDim={}", textLength, floatList.size());
            return EmbeddingVector.of(floatList);
        } catch (RuntimeException exception) {
            LOGGER.error("Embedding generation failed: textLength={}", textLength, exception);
            throw exception;
        }
    }
}
