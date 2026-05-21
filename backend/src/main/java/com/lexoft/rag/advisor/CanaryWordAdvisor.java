package com.lexoft.rag.advisor;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import java.util.List;
import java.util.UUID;

/**
 * Defends against system-prompt exfiltration: a random UUID canary word is appended
 * to the system message before the LLM call. If that word appears in the response the
 * model leaked the system prompt, and a safe denial message is returned instead.
 */
public class CanaryWordAdvisor implements CallAdvisor {

    private static final String DEFAULT_CANARY_FOUND_MESSAGE = "Canary word detected!";

    private final String canaryWordFoundMessage;

    private CanaryWordAdvisor(String canaryWordFoundMessage) {
        this.canaryWordFoundMessage = canaryWordFoundMessage;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain chain) {
        String canaryWord = UUID.randomUUID().toString();

        String originalSystemText = chatClientRequest.prompt().getSystemMessage().getText();
        String augmentedSystemText = originalSystemText + " (" + canaryWord + ")";

        ChatClientRequest advisedRequest = chatClientRequest.mutate()
                .prompt(chatClientRequest.prompt().augmentSystemMessage(augmentedSystemText))
                .build();

        ChatClientResponse response = chain.nextCall(advisedRequest);

        if (response.chatResponse().getResult().getOutput().getText().contains(canaryWord)) {
            return buildDenialResponse(advisedRequest);
        }

        return response;
    }

    private ChatClientResponse buildDenialResponse(ChatClientRequest request) {
        return new ChatClientResponse(
                ChatResponse.builder()
                        .generations(List.of(new Generation(new AssistantMessage(canaryWordFoundMessage))))
                        .build(),
                request.context());
    }

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    @Override
    public int getOrder() {
        return 0;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String canaryWordFoundMessage = DEFAULT_CANARY_FOUND_MESSAGE;

        public Builder canaryWordFoundMessage(String canaryWordFoundMessage) {
            this.canaryWordFoundMessage = canaryWordFoundMessage;
            return this;
        }

        public CanaryWordAdvisor build() {
            return new CanaryWordAdvisor(canaryWordFoundMessage);
        }
    }
}
