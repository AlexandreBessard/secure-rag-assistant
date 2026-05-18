package com.lexoft.rag.controller;

import com.lexoft.rag.model.ChatResult;
import com.lexoft.rag.model.HistoryMessage;
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
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
        when(chatService.ask(eq("Why is the sky blue?"), any(), any()))
                .thenReturn(new ChatResult("Because of Rayleigh scattering.", List.of()));
        when(evaluationService.evaluate(any(), any()))
                .thenReturn(new EvaluationResponse(true, 1.0f, "Relevant.", Collections.emptyMap()));

        mockMvc.perform(post("/ask")
                        .with(jwt().jwt(b -> b.subject("user-1")
                                .claim("realm_access", Map.of("roles", List.of("employee")))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question": "Why is the sky blue?"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Because of Rayleigh scattering."))
                .andExpect(jsonPath("$.relevant").value(true))
                .andExpect(jsonPath("$.score").value(1.0))
                .andExpect(jsonPath("$.feedback").value("Relevant."))
                .andExpect(jsonPath("$.sources").isArray());
    }

    @Test
    void history_returnsMessagesForAuthenticatedUser() throws Exception {
        when(chatService.getHistory("user-1"))
                .thenReturn(List.of(
                        new HistoryMessage("user", "What is the vacation policy?", List.of()),
                        new HistoryMessage("bot", "The policy is 25 days per year.", List.of())
                ));

        mockMvc.perform(get("/history")
                        .with(jwt().jwt(b -> b.subject("user-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].role").value("user"))
                .andExpect(jsonPath("$[0].text").value("What is the vacation policy?"))
                .andExpect(jsonPath("$[1].role").value("bot"))
                .andExpect(jsonPath("$[1].text").value("The policy is 25 days per year."));
    }

    @Test
    void deleteHistory_returnsNoContentAndDelegates() throws Exception {
        mockMvc.perform(delete("/history")
                        .with(jwt().jwt(b -> b.subject("user-1"))))
                .andExpect(status().isNoContent());
        verify(chatService).clearHistory("user-1");
    }

    @Test
    void ask_withNotRelevantEvaluation_returnsCorrectFlags() throws Exception {
        when(chatService.ask(any(), any(), any()))
                .thenReturn(new ChatResult("Unrelated answer.", List.of()));
        when(evaluationService.evaluate(any(), any()))
                .thenReturn(new EvaluationResponse(false, 0.0f, "Not relevant.", Collections.emptyMap()));

        mockMvc.perform(post("/ask")
                        .with(jwt().jwt(b -> b.subject("user-1")
                                .claim("realm_access", Map.of("roles", List.of("employee")))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question": "Why is the sky blue?"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.relevant").value(false))
                .andExpect(jsonPath("$.score").value(0.0));
    }
}
