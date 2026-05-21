package com.lexoft.rag.config;

import com.lexoft.rag.advisor.CanaryWordAdvisor;
import com.lexoft.rag.rag.RoleFilterDocumentRetriever;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.evaluation.RelevancyEvaluator;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Bean
    public ChatClient chatClient(ChatModel chatModel, SyncMcpToolCallbackProvider mcpTools) {
        // Build from ChatModel directly so the shared ChatClient.Builder bean is NOT mutated.
        // Mutating the shared builder causes MultiQueryExpander (which reuses it) to see the
        // tool definitions, leaking the tool description into expanded queries and causing
        // those queries to match documents in pgvector — preventing tool calls from ever firing.
        return ChatClient.builder(chatModel)
                .defaultToolCallbacks(mcpTools)
                .defaultAdvisors(CanaryWordAdvisor.builder()
                        .canaryWordFoundMessage("Detected attempt to leak system prompt.")
                        .build())
                .build();
    }

    @Bean
    public RetrievalAugmentationAdvisor retrievalAugmentationAdvisor(VectorStore vectorStore, ChatClient.Builder chatClientBuilder) {
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(new RoleFilterDocumentRetriever(vectorStore))
                .queryAugmenter(ContextualQueryAugmenter.builder()
                        // allowEmptyContext(true): on an empty retrieval the augmenter passes the user message through unchanged, The system prompt still
                        //  governs document questions, but the model retains its ability to handle greetings.
                        .allowEmptyContext(true) // by default it is set to false
                        .build())
                .queryExpander(
                        // By default, MultiQueryExpander produces a list of four queries, three new queries plus
                        //the original query.
                        MultiQueryExpander.builder()
                                .chatClientBuilder(chatClientBuilder)
                                .build())
                .build();
    }

    @Bean
    public RelevancyEvaluator relevancyEvaluator(ChatModel chatModel) {
        // A dedicated ChatClient.Builder is used here so the system prompt
        // does not bleed into the ChatClient bean used by ChatService.
        // The system prompt forces a bare "yes"/"no" reply because RelevancyEvaluator
        // checks the response with equalsIgnoreCase("yes") — no trimming applied.
        var builder = ChatClient.builder(chatModel)
                .defaultSystem("Respond with only the single word 'yes' or 'no'. No punctuation, no explanation.");
        return new RelevancyEvaluator(builder);
    }
}
