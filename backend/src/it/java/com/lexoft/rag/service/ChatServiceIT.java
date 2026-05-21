package com.lexoft.rag.service;

import com.lexoft.rag.common.security.Role;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ChatServiceIT {

    @Autowired
    private ChatService chatService;

    @Test
    void ask_returnsNonEmptyResponseFromBedrock() {
        var result = chatService.ask("Why is the sky blue?", Role.EMPLOYEE, "test-conversation");

        assertThat(result.answer()).isNotBlank();
        assertThat(result.sources()).isNotNull();
    }
}
