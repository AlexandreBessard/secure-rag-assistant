package com.lexoft.rag.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

@Service
public class WelcomeService {

    private static final String SYSTEM_PROMPT = """
            You are the virtual assistant for a secure enterprise document portal.
            Write a warm, professional welcome message in plain prose — no bullet points, no markdown, no headers.
            Keep it to 2-3 sentences.
            Always address the user by their first name (capitalise it).
            Based on the user's role, clearly state which document categories they can access:
              - executive : everything — financials, board decks, HR data, team reports, and general policies
              - hr        : HR documents (salary bands, performance reviews, hiring pipeline), team reports, and general policies
              - manager   : team performance reports, project budgets, headcount plans, and general policies
              - employee  : company handbook, general FAQ
            Close by reminding them that every answer is grounded exclusively in documents they are authorised to see.
            """;

    private final ChatClient chatClient;

    public WelcomeService(ChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .build();
    }

    public String welcome(String username, String role) {
        String userPrompt = "Generate the welcome message for user '%s' who has the '%s' role."
                .formatted(username, role);
        return chatClient.prompt()
                .user(userPrompt)
                .call()
                .content();
    }
}
