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
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatService {

    static final String DENIAL_PHRASE = "I don't have that information in the documents you are authorised to access.";

    private static final String SYSTEM_PROMPT = """
            You are a secure enterprise document assistant. You have three sources of information:
            1. Retrieved document context — use this to answer questions about company content.
            2. Conversation history — use this to answer questions about the current conversation
               (e.g. "what did I just ask?", "what was my first question?", "summarise our chat").
            3. Tools — call these when the user asks about the document repository
               (e.g. "how many documents", "how many company documents are there").
            Answer using ONLY these three sources. Do not use any external knowledge.
            If none of these sources contains enough information, respond with exactly this sentence and nothing else:
            "%s"
            Do not ask the user to provide documents. Do not suggest next steps. Do not add any explanation.
            Be concise and professional.
            """.formatted(DENIAL_PHRASE);

    private static final String CONVERSATION_ID_KEY = "chat_memory_conversation_id";

    private final ChatClient chatClient;
    private final RetrievalAugmentationAdvisor ragAdvisor;
    private final MessageChatMemoryAdvisor memoryAdvisor;
    private final MessageWindowChatMemory memory;
    private final ChatMemoryRepository chatMemoryRepository;
    private final JdbcTemplate jdbcTemplate;

    public ChatService(ChatClient chatClient, RetrievalAugmentationAdvisor ragAdvisor,
                       ChatMemoryRepository chatMemoryRepository, JdbcTemplate jdbcTemplate) {
        this.chatClient = chatClient;
        this.ragAdvisor = ragAdvisor;
        this.chatMemoryRepository = chatMemoryRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.memory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(20)
                .build();
        this.memoryAdvisor = MessageChatMemoryAdvisor.builder(this.memory).build();
    }

    public ChatResult ask(String question, String role, String conversationId) {
        // Phase 1 — tool-first: no RAG advisor, no memory advisor.
        // The LLM sees the original question and registered MCP tools and can call them directly.
        // Skipping memory here avoids writing a denial phrase into history if we fall through to Phase 2.
        String toolAnswer = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(question)
                .call()
                .content();

        boolean toolAnswered = toolAnswer != null && !toolAnswer.trim().startsWith(DENIAL_PHRASE);

        if (toolAnswered) {
            // Persist the exchange via the same MessageWindowChatMemory the advisor uses,
            // so history stays consistent across both phases.
            memory.add(conversationId, List.of(new UserMessage(question), new AssistantMessage(toolAnswer)));
            return new ChatResult(toolAnswer, List.of());
        }

        // Phase 2 — RAG fallback: full pipeline with retrieval and memory.
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
