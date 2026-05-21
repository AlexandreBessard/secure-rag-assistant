package com.lexoft.rag.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Configuration
public class EmbeddingConfig {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingConfig.class);
    private static final String MODEL_ID = "amazon.titan-embed-text-v2:0";

    @Value("${spring.ai.bedrock.aws.region:eu-west-1}")
    private String bedrockRegion;

    @Bean
    public BedrockRuntimeClient bedrockRuntimeClient() {
        return BedrockRuntimeClient.builder()
                .region(Region.of(bedrockRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Bean
    @Primary
    public EmbeddingModel embeddingModel(BedrockRuntimeClient bedrockRuntimeClient,
                                         ObjectMapper objectMapper) {
        return new EmbeddingModel() {

            @Override
            public EmbeddingResponse call(EmbeddingRequest request) {
                List<Embedding> embeddings = new ArrayList<>();
                List<String> texts = request.getInstructions();
                for (int i = 0; i < texts.size(); i++) {
                    embeddings.add(new Embedding(embed(texts.get(i)), i));
                }
                return new EmbeddingResponse(embeddings);
            }

            @Override
            public float[] embed(String text) {
                try {
                    String body = objectMapper.writeValueAsString(Map.of("inputText", text));
                    var response = bedrockRuntimeClient.invokeModel(r -> r
                            .modelId(MODEL_ID)
                            .body(SdkBytes.fromUtf8String(body))
                            .contentType("application/json")
                            .accept("application/json"));

                    @SuppressWarnings("unchecked")
                    List<Double> raw = (List<Double>) objectMapper
                            .readValue(response.body().asUtf8String(), Map.class)
                            .get("embedding");

                    float[] vector = new float[raw.size()];
                    for (int i = 0; i < raw.size(); i++) {
                        vector[i] = raw.get(i).floatValue();
                    }
                    log.debug("Embedded {} chars → {} dims", text.length(), vector.length);
                    return vector;
                } catch (Exception e) {
                    throw new RuntimeException("Titan V2 embedding failed", e);
                }
            }

            @Override
            public float[] embed(org.springframework.ai.document.Document document) {
                return embed(Objects.requireNonNull(document.getText()));
            }

            @Override
            public int dimensions() {
                return 1024;
            }
        };
    }
}