package com.lexoft.rag.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/*
  RoleFilterDocumentRetriever (com.lexoft.rag.rag) — implements org.springframework.ai.rag.retrieval.search.DocumentRetriever.
  Reads required_role from Query.context() (which RetrievalAugmentationAdvisor populates from AdvisedRequest
  params), applies it as a metadata filter expression, and falls back to "employee" when absent — the safe default.
 */
public class RoleFilterDocumentRetriever implements DocumentRetriever {

    private static final Logger log = LoggerFactory.getLogger(RoleFilterDocumentRetriever.class);

    public static final String ROLE_CONTEXT_KEY = "required_role";
    private static final String DEFAULT_ROLE = "employee";
    private static final int TOP_K = 5;
    private static final double SIMILARITY_THRESHOLD = 0.5;

    // Hierarchical access: a role may read documents tagged for its own level and all levels below.
    // executive sees everything; hr/manager each see their own tier plus employee; employee sees only employee.
    private static final Map<String, List<String>> ACCESSIBLE_ROLES = Map.of(
            "executive", List.of("executive", "manager", "hr", "employee"),
            "manager",   List.of("manager", "employee"),
            "hr",        List.of("hr", "manager", "employee"),
            "employee",  List.of("employee")
    );

    private final VectorStore vectorStore;

    public RoleFilterDocumentRetriever(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public List<Document> retrieve(Query query) {
        Map<String, Object> ctx = query.context();
        String role = (String) ctx.getOrDefault(ROLE_CONTEXT_KEY, DEFAULT_ROLE);

        List<String> accessibleRoles = ACCESSIBLE_ROLES.getOrDefault(role, List.of(role));
        String filterExpr = buildFilterExpression(accessibleRoles);

        List<Document> docs = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query.text())
                        .filterExpression(filterExpr)
                        .topK(TOP_K)
                        //.similarityThreshold(SIMILARITY_THRESHOLD)
                        .build()
        );
        log.info("RAG retrieve — role='{}' accessible={} query='{}' threshold={} hits={}",
                role, accessibleRoles, query.text(), SIMILARITY_THRESHOLD, docs.size());
        return docs;
    }

    private static String buildFilterExpression(List<String> roles) {
        if (roles.size() == 1) {
            return "required_role == '" + roles.get(0) + "'";
        }
        String values = roles.stream()
                .map(r -> "'" + r + "'")
                .collect(Collectors.joining(", "));
        return "required_role in [" + values + "]";
    }
}
