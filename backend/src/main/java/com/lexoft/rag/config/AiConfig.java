package com.lexoft.rag.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.evaluation.RelevancyEvaluator;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
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
