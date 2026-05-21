package com.lexoft.rag.service;

import com.lexoft.rag.model.ChatResult;
import com.lexoft.rag.model.HistoryMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    private static final String DENIAL_PHRASE =
            "I don't have that information in the documents you are authorised to access.";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(chatService, "denialPhrase", DENIAL_PHRASE);
        ReflectionTestUtils.setField(chatService, "systemPrompt", "You are a helpful assistant.");
    }

    @Mock
    private ChatClient chatClient;
    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;
    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;
    @Mock
    private RetrievalAugmentationAdvisor ragAdvisor;
    @Mock
    private ChatMemoryRepository chatMemoryRepository;
    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private ChatService chatService;

    private ChatClientResponse buildClientResponse(String answer, List<Document> docs) {
        ChatResponse chatResponse = new ChatResponse(List.of(new Generation(new AssistantMessage(answer))));
        Map<String, Object> context = new HashMap<>();
        context.put(RetrievalAugmentationAdvisor.DOCUMENT_CONTEXT, docs);
        return new ChatClientResponse(chatResponse, context);
    }

    private void stubChatChain(ChatClientResponse clientResponse) {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.advisors(any(Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.chatClientResponse()).thenReturn(clientResponse);
    }

    @Test
    void ask_delegatesQuestionToModelAndReturnsContent() {
        stubChatChain(buildClientResponse("The sky is blue because of Rayleigh scattering.", List.of()));

        ChatResult result = chatService.ask("Why is the sky blue?", "employee", "conv-1");

        assertThat(result.answer()).isEqualTo("The sky is blue because of Rayleigh scattering.");
        assertThat(result.sources()).isEmpty();
    }

    @Test
    void ask_returnsSourcesFromRetrievedDocuments() {
        Document doc = Document.builder()
                .text("Some content about finances.")
                .metadata(Map.of("source", "annual-report.pdf", "document_id", "doc-1"))
                .build();
        stubChatChain(buildClientResponse("Answer based on document.", List.of(doc)));

        ChatResult result = chatService.ask("What are the financials?", "executive", "conv-1");

        assertThat(result.sources()).hasSize(1);
        assertThat(result.sources().get(0).documentName()).isEqualTo("annual-report.pdf");
        assertThat(result.sources().get(0).documentId()).isEqualTo("doc-1");
    }

    @Test
    void ask_deduplicatesSourcesFromSameDocument() {
        Map<String, Object> meta = Map.of("source", "report.pdf", "document_id", "doc-1");
        Document doc1 = Document.builder().text("Chunk 1").metadata(meta).build();
        Document doc2 = Document.builder().text("Chunk 2").metadata(meta).build();
        stubChatChain(buildClientResponse("Answer.", List.of(doc1, doc2)));

        ChatResult result = chatService.ask("Question?", "employee", "conv-1");

        assertThat(result.sources()).hasSize(1);
    }

    @Test
    void ask_suppressesSourcesWhenModelReturnsDenialMessage() {
        Document doc = Document.builder()
                .text("Confidential HR data.")
                .metadata(Map.of("source", "hr-policy.pdf", "document_id", "doc-2"))
                .build();
        stubChatChain(buildClientResponse(DENIAL_PHRASE, List.of(doc)));

        ChatResult result = chatService.ask("What is the CEO salary?", "employee", "conv-1");

        assertThat(result.sources()).isEmpty();
    }

    @Test
    void ask_propagatesEmptyResponseFromModel() {
        stubChatChain(buildClientResponse("", List.of()));

        ChatResult result = chatService.ask("test", "employee", "conv-1");

        assertThat(result.answer()).isEmpty();
        assertThat(result.sources()).isEmpty();
    }

    @Test
    void clearHistory_delegatesToRepository() {
        chatService.clearHistory("conv-1");
        verify(chatMemoryRepository).deleteByConversationId("conv-1");
    }

    @Test
    void getHistory_filtersSystemMessagesAndMapsRoles() {
        when(chatMemoryRepository.findByConversationId("conv-1"))
                .thenReturn(List.of(
                        new SystemMessage("system prompt"),
                        new UserMessage("Hello"),
                        new AssistantMessage("Hi there")
                ));

        List<HistoryMessage> history = chatService.getHistory("conv-1");

        assertThat(history).hasSize(2);
        assertThat(history.get(0).role()).isEqualTo("user");
        assertThat(history.get(0).text()).isEqualTo("Hello");
        assertThat(history.get(0).sources()).isEmpty();
        assertThat(history.get(1).role()).isEqualTo("bot");
        assertThat(history.get(1).text()).isEqualTo("Hi there");
        assertThat(history.get(1).sources()).isEmpty();
    }
}
