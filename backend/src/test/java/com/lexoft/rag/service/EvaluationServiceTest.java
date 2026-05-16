package com.lexoft.rag.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.evaluation.RelevancyEvaluator;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EvaluationServiceTest {

    @Mock
    private RelevancyEvaluator evaluator;

    @InjectMocks
    private EvaluationService evaluationService;

    @Test
    void evaluate_whenRelevant_returnsPassingResponse() {
        when(evaluator.evaluate(any(EvaluationRequest.class)))
                .thenReturn(new EvaluationResponse(true, 1.0f, "Relevant answer.", Collections.emptyMap()));

        EvaluationResponse result = evaluationService.evaluate("Why is the sky blue?", "Because of Rayleigh scattering.");

        assertThat(result.isPass()).isTrue();
        assertThat(result.getScore()).isEqualTo(1.0f);
        assertThat(result.getFeedback()).isEqualTo("Relevant answer.");
    }

    @Test
    void evaluate_whenNotRelevant_returnsFailingResponse() {
        when(evaluator.evaluate(any(EvaluationRequest.class)))
                .thenReturn(new EvaluationResponse(false, 0.0f, "Not relevant.", Collections.emptyMap()));

        EvaluationResponse result = evaluationService.evaluate("Why is the sky blue?", "Cats are mammals.");

        assertThat(result.isPass()).isFalse();
        assertThat(result.getScore()).isEqualTo(0.0f);
    }

    @Test
    void evaluate_passesQuestionAndAnswerToEvaluator() {
        when(evaluator.evaluate(any()))
                .thenReturn(new EvaluationResponse(true, 1.0f, "", Collections.emptyMap()));
        var captor = ArgumentCaptor.forClass(EvaluationRequest.class);

        evaluationService.evaluate("Why is the sky blue?", "Rayleigh scattering.");

        verify(evaluator).evaluate(captor.capture());
        assertThat(captor.getValue().getUserText()).isEqualTo("Why is the sky blue?");
        assertThat(captor.getValue().getResponseContent()).isEqualTo("Rayleigh scattering.");
    }
}
