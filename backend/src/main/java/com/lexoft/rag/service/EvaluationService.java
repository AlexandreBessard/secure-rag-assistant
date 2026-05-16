package com.lexoft.rag.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.evaluation.RelevancyEvaluator;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;
import org.springframework.stereotype.Service;

@Service
public class EvaluationService {

    private final RelevancyEvaluator evaluator;

    public EvaluationService(ChatClient.Builder chatClientBuilder) {
        this.evaluator = new RelevancyEvaluator(chatClientBuilder);
    }

    public EvaluationResponse evaluate(String question, String answer) {
        return evaluator.evaluate(new EvaluationRequest(question, answer));
    }
}
