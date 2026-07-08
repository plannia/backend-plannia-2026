package upc.com.pe.backendplannia.profile.application.internal.outboundservices.ai;

import upc.com.pe.backendplannia.shared.domain.model.valueobjects.EmbeddingVector;

public interface ProfileEmbeddingService {
    EmbeddingVector generateEmbedding(String text);
}
