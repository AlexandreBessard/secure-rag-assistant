package com.lexoft.rag.service;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.evaluation.RelevancyEvaluator;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class EvaluationServiceIT {

    private static final String EVALUATOR_SYSTEM_PROMPT =
            "You are an evaluator. Respond with only the single word 'yes' or 'no'. No punctuation, no explanation.";

    private static final Document SKY_CONTEXT = new Document(
            "The sky appears blue due to Rayleigh scattering. When sunlight enters Earth's atmosphere, " +
            "gas molecules scatter shorter wavelengths of light (blue) much more than longer wavelengths (red). " +
            "This scattered blue light reaches our eyes from all directions, making the sky look blue."
    );

    @Autowired
    private ChatService chatService;

    @Autowired
    private ChatModel chatModel;

    @Test
    void evaluateRelevancy_relevantAnswerPassesEvaluation() {
        var question = "Why is the sky blue?";
        var answer = chatService.ask(question);

        var evaluationBuilder = ChatClient.builder(chatModel).defaultSystem(EVALUATOR_SYSTEM_PROMPT);
        var evaluator = new RelevancyEvaluator(evaluationBuilder);
        EvaluationResponse response = evaluator.evaluate(
                new EvaluationRequest(question, List.of(SKY_CONTEXT), answer));

        Assertions.assertThat(response.isPass())
                .withFailMessage("""
                        ========================================
                        The answer "%s"
                        is not considered relevant to the question
                        "%s".
                        ========================================
                        """, answer, question)
                .isTrue();
    }
}
