package com.lexoft.rag.service;

import com.lexoft.rag.model.ChatResult;
import com.lexoft.rag.model.HistoryMessage;
import com.lexoft.rag.model.Source;
import com.lexoft.rag.rag.RoleFilterDocumentRetriever;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatService {

    static final String DENIAL_PHRASE = "I don't have that information in the documents you are authorised to access.";

    private static final String SYSTEM_PROMPT = """
            You are a secure enterprise document assistant. You have two sources of information:
            1. Retrieved document context — use this to answer questions about company content.
            2. Conversation history — use this to answer questions about the current conversation
               (e.g. "what did I just ask?", "what was my first question?", "summarise our chat").
            Answer using ONLY these two sources. Do not use any external knowledge.
            If neither source contains enough information, respond with exactly this sentence and nothing else:
            "%s"
            Do not ask the user to provide documents. Do not suggest next steps. Do not add any explanation.
            Be concise and professional.
            """.formatted(DENIAL_PHRASE);

    private static final String CONVERSATION_ID_KEY = "chat_memory_conversation_id";

    private final ChatClient chatClient;
    private final RetrievalAugmentationAdvisor ragAdvisor;
    private final MessageChatMemoryAdvisor memoryAdvisor;
    private final ChatMemoryRepository chatMemoryRepository;
    private final JdbcTemplate jdbcTemplate;

    public ChatService(ChatClient chatClient, RetrievalAugmentationAdvisor ragAdvisor,
                       ChatMemoryRepository chatMemoryRepository, JdbcTemplate jdbcTemplate) {
        this.chatClient = chatClient;
        this.ragAdvisor = ragAdvisor;
        this.chatMemoryRepository = chatMemoryRepository;
        this.jdbcTemplate = jdbcTemplate;
        var memory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(20)
                .build();
        this.memoryAdvisor = MessageChatMemoryAdvisor.builder(memory).build();
    }

    public ChatResult ask(String question, String role, String conversationId) {
        ChatClientResponse clientResponse = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(question)
                .advisors(spec -> spec
                        .param(RoleFilterDocumentRetriever.ROLE_CONTEXT_KEY, role)
                        .param(CONVERSATION_ID_KEY, conversationId)
                        .advisors(ragAdvisor, memoryAdvisor))
                .call()
                .chatClientResponse();

        assert clientResponse.chatResponse() != null;
        String answer = clientResponse.chatResponse().getResult().getOutput().getText();

        Object rawDocs = clientResponse.context().get(RetrievalAugmentationAdvisor.DOCUMENT_CONTEXT);
        @SuppressWarnings("unchecked")
        List<Document> docs = rawDocs instanceof List<?> ? (List<Document>) rawDocs : List.of();

        boolean denied = answer != null && answer.trim().startsWith(DENIAL_PHRASE);

        List<Source> sources = denied ? List.of() : docs.stream()
                .map(doc -> new Source(
                        (String) doc.getMetadata().getOrDefault("source", ""),
                        (String) doc.getMetadata().getOrDefault("document_id", "")
                ))
                .filter(s -> !s.documentName().isBlank())
                .distinct()
                .toList();

        if (!sources.isEmpty()) {
            saveSources(conversationId, answer, sources);
        }

        return new ChatResult(answer, sources);
    }

    public void clearHistory(String conversationId) {
        chatMemoryRepository.deleteByConversationId(conversationId);
        jdbcTemplate.update("DELETE FROM RAG_SOURCES WHERE conversation_id = ?", conversationId);
    }

    public List<HistoryMessage> getHistory(String conversationId) {
        return chatMemoryRepository.findByConversationId(conversationId).stream()
                .filter(m -> m.getMessageType() == MessageType.USER
                        || m.getMessageType() == MessageType.ASSISTANT)
                .map(m -> {
                    List<Source> sources = m.getMessageType() == MessageType.ASSISTANT
                            ? loadSources(conversationId, m.getText())
                            : List.of();
                    return new HistoryMessage(
                            m.getMessageType() == MessageType.USER ? "user" : "bot",
                            m.getText(),
                            sources);
                })
                .toList();
    }

    private void saveSources(String conversationId, String answer, List<Source> sources) {
        for (Source source : sources) {
            jdbcTemplate.update(
                    "INSERT INTO RAG_SOURCES (conversation_id, message_text, document_name, document_id) VALUES (?, ?, ?, ?)",
                    conversationId, answer, source.documentName(), source.documentId());
        }
    }

    private List<Source> loadSources(String conversationId, String messageText) {
        List<Source> result = jdbcTemplate.query(
                "SELECT document_name, document_id FROM RAG_SOURCES WHERE conversation_id = ? AND message_text = ?",
                (rs, rowNum) -> new Source(rs.getString("document_name"), rs.getString("document_id")),
                conversationId, messageText);
        return result != null ? result : List.of();
    }
}
