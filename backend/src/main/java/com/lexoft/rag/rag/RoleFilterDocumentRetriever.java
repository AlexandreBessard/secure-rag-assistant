package com.lexoft.rag.rag;

import com.lexoft.rag.common.security.Role;
import com.lexoft.rag.common.security.RoleHierarchy;
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

public class RoleFilterDocumentRetriever implements DocumentRetriever {

    private static final Logger log = LoggerFactory.getLogger(RoleFilterDocumentRetriever.class);

    public static final String ROLE_CONTEXT_KEY = "required_role";
    private static final int TOP_K = 5;

    private final VectorStore vectorStore;

    public RoleFilterDocumentRetriever(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public List<Document> retrieve(Query query) {
        Map<String, Object> ctx = query.context();
        Role role = (Role) ctx.getOrDefault(ROLE_CONTEXT_KEY, RoleHierarchy.DEFAULT);

        List<Role> accessibleRoles = RoleHierarchy.ACCESSIBLE.getOrDefault(role, List.of(role));
        String filterExpr = buildFilterExpression(accessibleRoles);

        List<Document> docs = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query.text())
                        .filterExpression(filterExpr)
                        .topK(TOP_K)
                        .build()
        );
        log.info("RAG retrieve — role='{}' accessible={} query='{}' hits={}",
                role, accessibleRoles, query.text(), docs.size());
        return docs;
    }

    private static String buildFilterExpression(List<Role> roles) {
        if (roles.size() == 1) {
            return "required_role == '" + roles.get(0).value() + "'";
        }
        String values = roles.stream()
                .map(r -> "'" + r.value() + "'")
                .collect(Collectors.joining(", "));
        return "required_role in [" + values + "]";
    }
}
