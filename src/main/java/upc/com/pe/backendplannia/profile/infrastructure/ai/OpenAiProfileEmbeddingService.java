package upc.com.pe.backendplannia.profile.infrastructure.ai;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.stereotype.Service;
import upc.com.pe.backendplannia.profile.application.internal.outboundservices.ai.ProfileEmbeddingService;
import upc.com.pe.backendplannia.shared.domain.model.valueobjects.EmbeddingVector;

import java.util.ArrayList;
import java.util.List;

@Service
public class OpenAiProfileEmbeddingService implements ProfileEmbeddingService {
    private final EmbeddingModel embeddingModel;

    public OpenAiProfileEmbeddingService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    @Override
    public EmbeddingVector generateEmbedding(String text) {
        var request = new EmbeddingRequest(List.of(text), null);
        var response = embeddingModel.call(request);
        float[] vector = response.getResults().getFirst().getOutput();

        List<Float> floatList = new ArrayList<>();
        for (float f : vector) floatList.add(f);
        return EmbeddingVector.of(floatList);
    }
}
