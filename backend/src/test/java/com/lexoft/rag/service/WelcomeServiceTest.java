package com.lexoft.rag.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WelcomeServiceTest {

    @Mock
    private ChatModel chatModel;
    @Mock
    private ChatMemoryRepository chatMemoryRepository;

    private WelcomeService service;

    @BeforeEach
    void setUp() {
        service = new WelcomeService(chatModel, chatMemoryRepository);
    }

    private ChatResponse response(String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
    }

    @Test
    void welcome_returnsTextFromModel() {
        when(chatModel.call(any(Prompt.class))).thenReturn(response("Welcome, Alice!"));
        when(chatMemoryRepository.findByConversationId("conv-1")).thenReturn(List.of());

        String result = service.welcome("alice", "executive", "conv-1");

        assertThat(result).isEqualTo("Welcome, Alice!");
    }

    @Test
    void welcome_persistsMessageOnFreshConversation() {
        when(chatModel.call(any(Prompt.class))).thenReturn(response("Welcome, Bob!"));
        when(chatMemoryRepository.findByConversationId("conv-1")).thenReturn(List.of());

        service.welcome("bob", "hr", "conv-1");

        verify(chatMemoryRepository).saveAll(eq("conv-1"), anyList());
    }

    @Test
    void welcome_doesNotPersistWhenConversationAlreadyHasMessages() {
        when(chatModel.call(any(Prompt.class))).thenReturn(response("Welcome back!"));
        when(chatMemoryRepository.findByConversationId("conv-1"))
                .thenReturn(List.of(new AssistantMessage("previous welcome")));

        service.welcome("carol", "manager", "conv-1");

        verify(chatMemoryRepository, never()).saveAll(anyString(), anyList());
    }

    @Test
    void welcome_checksConversationWithCorrectId() {
        when(chatModel.call(any(Prompt.class))).thenReturn(response("Hi Dave!"));
        when(chatMemoryRepository.findByConversationId("user-sub-dave")).thenReturn(List.of());

        service.welcome("dave", "employee", "user-sub-dave");

        verify(chatMemoryRepository).findByConversationId("user-sub-dave");
    }
}
