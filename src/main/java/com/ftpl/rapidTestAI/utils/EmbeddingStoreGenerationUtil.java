package com.ftpl.rapidTestAI.utils;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import java.util.List;

public class EmbeddingStoreGenerationUtil {

    public static InMemoryEmbeddingStore<TextSegment> createEmbeddingStore(final String resourcesPath) {
        final List<Document> documents = FileSystemDocumentLoader.loadDocumentsRecursively(resourcesPath);
        final InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        EmbeddingStoreIngestor.ingest(documents, embeddingStore);
        return embeddingStore;
    }
}
