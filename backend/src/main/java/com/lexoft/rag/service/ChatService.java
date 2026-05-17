package com.lexoft.rag.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private static final String SYSTEM_PROMPT = """
            You are a secure enterprise document assistant.
            Answer the user's question using ONLY the context provided below.
            If the context does not contain enough information, say exactly:
            "I don't have that information in the documents you are authorised to access."
            Never reference information outside the provided context.
            Be concise and professional.
            """;

    private static final String CONVERSATION_ID_KEY = "chat_memory_conversation_id";

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final MessageChatMemoryAdvisor memoryAdvisor;

    public ChatService(ChatClient chatClient, VectorStore vectorStore) {
        this.chatClient = chatClient;
        this.vectorStore = vectorStore;
        var memory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20) // -> keep the latest 20 messages in memory
                .build();
        this.memoryAdvisor = MessageChatMemoryAdvisor.builder(memory).build();
    }

    public String ask(String question, String role, String conversationId) {
        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(question)
                .advisors(spec -> spec
                        .param(CONVERSATION_ID_KEY, conversationId)
                        .advisors(
                                memoryAdvisor,
                                QuestionAnswerAdvisor.builder(vectorStore)
                                        .searchRequest(SearchRequest.builder()
                                                .filterExpression("required_role == '" + role + "'")
                                                .topK(5)
                                                .build())
                                        .build()
                        ))
                .call()
                .content();
    }
}
