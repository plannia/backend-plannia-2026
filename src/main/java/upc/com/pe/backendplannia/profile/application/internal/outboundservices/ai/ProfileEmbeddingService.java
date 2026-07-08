package upc.com.pe.backendplannia.profile.application.internal.outboundservices.ai;

import upc.com.pe.backendplannia.shared.domain.model.valueobjects.EmbeddingVector;

import java.util.List;

public interface ProfileEmbeddingService {
    EmbeddingVector generateEmbedding(String text);

    /** Embebe varios textos en una sola llamada (un embedding por ítem, en el mismo orden). */
    List<EmbeddingVector> generateEmbeddings(List<String> texts);
}
