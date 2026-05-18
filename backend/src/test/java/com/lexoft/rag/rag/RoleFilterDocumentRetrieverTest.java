package com.lexoft.rag.rag;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleFilterDocumentRetrieverTest {

    @Mock
    private VectorStore vectorStore;

    @InjectMocks
    private RoleFilterDocumentRetriever retriever;

    private String captureFilterExpression(Query query) {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());
        retriever.retrieve(query);
        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(captor.capture());
        return captor.getValue().getFilterExpression().toString();
    }

    @Test
    void retrieve_executiveCanAccessAllRoles() {
        Query query = Query.builder()
                .text("What is the vacation policy?")
                .context(Map.of(RoleFilterDocumentRetriever.ROLE_CONTEXT_KEY, "executive"))
                .build();

        String filter = captureFilterExpression(query);

        assertThat(filter).contains("executive");
        assertThat(filter).contains("manager");
        assertThat(filter).contains("hr");
        assertThat(filter).contains("employee");
    }

    @Test
    void retrieve_managerCanAccessManagerAndEmployee() {
        Query query = Query.builder()
                .text("What are team lead responsibilities?")
                .context(Map.of(RoleFilterDocumentRetriever.ROLE_CONTEXT_KEY, "manager"))
                .build();

        String filter = captureFilterExpression(query);

        assertThat(filter).contains("manager");
        assertThat(filter).contains("employee");
        assertThat(filter).doesNotContain("executive");
        assertThat(filter).doesNotContain("hr");
    }

    @Test
    void retrieve_hrCanAccessHrAndEmployee() {
        Query query = Query.builder()
                .text("What is the hiring process?")
                .context(Map.of(RoleFilterDocumentRetriever.ROLE_CONTEXT_KEY, "hr"))
                .build();

        String filter = captureFilterExpression(query);

        assertThat(filter).contains("hr");
        assertThat(filter).contains("employee");
        assertThat(filter).doesNotContain("executive");
        assertThat(filter).doesNotContain("manager");
    }

    @Test
    void retrieve_employeeCanOnlyAccessEmployeeDocuments() {
        Query query = Query.builder()
                .text("What are the working hours?")
                .context(Map.of(RoleFilterDocumentRetriever.ROLE_CONTEXT_KEY, "employee"))
                .build();

        String filter = captureFilterExpression(query);

        assertThat(filter).contains("employee");
        assertThat(filter).doesNotContain("executive");
        assertThat(filter).doesNotContain("manager");
        assertThat(filter).doesNotContain("hr");
    }

    @Test
    void retrieve_fallsBackToEmployeeWhenContextKeyAbsent() {
        Query query = Query.builder().text("Anything").context(Map.of()).build();

        String filter = captureFilterExpression(query);

        assertThat(filter).contains("employee");
        assertThat(filter).doesNotContain("executive");
    }

    @Test
    void retrieve_fallsBackToEmployeeWhenContextIsNull() {
        Query query = new Query("Anything");

        String filter = captureFilterExpression(query);

        assertThat(filter).contains("employee");
    }

    @Test
    void retrieve_returnsDocumentsFromVectorStore() {
        Document doc = Document.builder()
                .text("Revenue for Q1 2025.")
                .metadata(Map.of("source", "financials.pdf", "document_id", "doc-1"))
                .build();
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(doc));
        Query query = Query.builder()
                .text("Q1 revenue")
                .context(Map.of(RoleFilterDocumentRetriever.ROLE_CONTEXT_KEY, "executive"))
                .build();

        List<Document> result = retriever.retrieve(query);

        assertThat(result).containsExactly(doc);
    }
}
