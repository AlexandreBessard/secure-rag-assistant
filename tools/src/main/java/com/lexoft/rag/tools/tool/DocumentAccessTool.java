package com.lexoft.rag.tools.tool;

import com.lexoft.rag.common.security.RoleHierarchy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;

import java.util.List;
import java.util.Map;

@Component
public class DocumentAccessTool {

    private static final Logger log = LoggerFactory.getLogger(DocumentAccessTool.class);

    private final S3Client s3Client;
    private final String bucket;

    public DocumentAccessTool(S3Client s3Client, @Value("${app.s3.bucket-name}") String bucket) {
        this.s3Client = s3Client;
        this.bucket = bucket;
    }

    @PreAuthorize("hasAnyRole('executive', 'hr', 'manager', 'employee')")
    @Tool(name = "countAccessibleDocuments",
          description = "Returns the number of company documents the current user is authorised to access, based on their role. " +
                        "Documents are stored in the company S3 bucket, organised by role-based prefixes (executive/, hr/, manager/, employee/). " +
                        "The count reflects only the folders the user's role grants access to, following the privilege hierarchy: " +
                        "executive > hr/manager > employee.")
    public String countAccessibleDocuments() {
        String role = resolveUserRole();
        List<String> prefixes = RoleHierarchy.ACCESSIBLE.getOrDefault(role, List.of(RoleHierarchy.DEFAULT));

        log.info("Counting documents — role='{}' prefixes={} bucket={}", role, prefixes, bucket);

        long total = prefixes.stream()
                .mapToLong(prefix ->
                        s3Client.listObjectsV2Paginator(
                                        ListObjectsV2Request.builder()
                                                .bucket(bucket)
                                                .prefix(prefix + "/")
                                                .build()
                                )
                                .stream()
                                .mapToLong(response -> response.contents().stream()
                                        .filter(obj -> !obj.key().endsWith("/"))
                                        .count())
                                .sum()
                )
                .sum();

        log.info("Accessible document count for role='{}': {}", role, total);
        return String.format("You have access to %d document(s) as a %s.", total, role);
    }

    private String resolveUserRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
            return RoleHierarchy.DEFAULT;
        }
        Map<String, Object> realmAccess = jwtAuth.getToken().getClaimAsMap("realm_access");
        if (realmAccess == null) return RoleHierarchy.DEFAULT;

        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) realmAccess.get("roles");
        return RoleHierarchy.resolve(roles);
    }
}
