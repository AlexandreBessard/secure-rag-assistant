package com.lexoft.rag.ingestion.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    private final VectorStore vectorStore;
    private final TokenTextSplitter splitter = new TokenTextSplitter();

    public IngestionService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public int ingest(String documentId, String requiredRole, String filename, byte[] fileBytes) {
        List<Document> raw = new TikaDocumentReader(new ByteArrayResource(fileBytes, filename)).get();

        List<Document> chunks = splitter.apply(raw);
        chunks.forEach(chunk -> {
            chunk.getMetadata().put("document_id", documentId);
            chunk.getMetadata().put("required_role", requiredRole);
            chunk.getMetadata().put("source", filename);
        });

        vectorStore.accept(chunks);
        log.info("Stored {} chunks for document_id='{}' (role={})", chunks.size(), documentId, requiredRole);
        return chunks.size();
    }
}
