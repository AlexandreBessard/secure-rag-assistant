package com.lexoft.rag.controller;

import com.lexoft.rag.service.ChatService;
import com.lexoft.rag.service.EvaluationService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.evaluation.EvaluationResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AskController.class)
class AskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatService chatService;

    @MockitoBean
    private EvaluationService evaluationService;

    @Test
    void ask_returnsAnswerWithEvaluation() throws Exception {
        when(chatService.ask("Why is the sky blue?"))
                .thenReturn("Because of Rayleigh scattering.");
        when(evaluationService.evaluate(any(), any()))
                .thenReturn(new EvaluationResponse(true, 1.0f, "Relevant.", Collections.emptyMap()));

        mockMvc.perform(post("/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question": "Why is the sky blue?"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Because of Rayleigh scattering."))
                .andExpect(jsonPath("$.relevant").value(true))
                .andExpect(jsonPath("$.score").value(1.0))
                .andExpect(jsonPath("$.feedback").value("Relevant."));
    }

    @Test
    void ask_withNotRelevantEvaluation_returnsCorrectFlags() throws Exception {
        when(chatService.ask(any())).thenReturn("Unrelated answer.");
        when(evaluationService.evaluate(any(), any()))
                .thenReturn(new EvaluationResponse(false, 0.0f, "Not relevant.", Collections.emptyMap()));

        mockMvc.perform(post("/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question": "Why is the sky blue?"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.relevant").value(false))
                .andExpect(jsonPath("$.score").value(0.0));
    }
}
