package com.lexoft.rag.tools.tool;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentAccessToolTest {

    @Mock
    private S3Client s3Client;

    private DocumentAccessTool tool;

    @BeforeEach
    void setUp() {
        tool = new DocumentAccessTool(s3Client, "test-bucket");
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void setJwtRole(String... roles) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", "user-1")
                .claim("realm_access", Map.of("roles", Arrays.asList(roles)))
                .build();
        SecurityContextHolder.getContext()
                .setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
    }

    private ListObjectsV2Iterable paginatorWith(String... keys) {
        List<S3Object> objects = Arrays.stream(keys)
                .map(k -> S3Object.builder().key(k).build())
                .toList();
        ListObjectsV2Response page = ListObjectsV2Response.builder().contents(objects).build();
        ListObjectsV2Iterable iterable = mock(ListObjectsV2Iterable.class);
        doReturn(Stream.of(page)).when(iterable).stream();
        return iterable;
    }

    @Test
    void countAccessibleDocuments_executiveSeesAllFourPrefixes() {
        setJwtRole("executive");
        when(s3Client.listObjectsV2Paginator(any(ListObjectsV2Request.class)))
                .thenReturn(paginatorWith("executive/uuid/a.pdf"))
                .thenReturn(paginatorWith("manager/uuid/b.pdf"))
                .thenReturn(paginatorWith("hr/uuid/c.pdf"))
                .thenReturn(paginatorWith("employee/uuid/d.pdf"));

        String result = tool.countAccessibleDocuments();

        assertThat(result).contains("4");
        assertThat(result).contains("executive");
        verify(s3Client, times(4)).listObjectsV2Paginator(any(ListObjectsV2Request.class));
    }

    @Test
    void countAccessibleDocuments_employeeSeesOnlyEmployeePrefix() {
        setJwtRole("employee");
        when(s3Client.listObjectsV2Paginator(any(ListObjectsV2Request.class)))
                .thenReturn(paginatorWith("employee/uuid/a.pdf", "employee/uuid/b.pdf"));

        String result = tool.countAccessibleDocuments();

        assertThat(result).contains("2");
        assertThat(result).contains("employee");
        verify(s3Client, times(1)).listObjectsV2Paginator(any(ListObjectsV2Request.class));
    }

    @Test
    void countAccessibleDocuments_hrSeesHrManagerAndEmployee() {
        setJwtRole("hr");
        when(s3Client.listObjectsV2Paginator(any(ListObjectsV2Request.class)))
                .thenReturn(paginatorWith("hr/uuid/a.pdf"))
                .thenReturn(paginatorWith("manager/uuid/b.pdf"))
                .thenReturn(paginatorWith("employee/uuid/c.pdf"));

        String result = tool.countAccessibleDocuments();

        assertThat(result).contains("3");
        verify(s3Client, times(3)).listObjectsV2Paginator(any(ListObjectsV2Request.class));
    }

    @Test
    void countAccessibleDocuments_directoryMarkersAreExcluded() {
        setJwtRole("employee");
        when(s3Client.listObjectsV2Paginator(any(ListObjectsV2Request.class)))
                .thenReturn(paginatorWith("employee/", "employee/uuid/doc.pdf"));

        String result = tool.countAccessibleDocuments();

        assertThat(result).contains("1");
    }

    @Test
    void countAccessibleDocuments_fallsBackToEmployeeWhenNoAuthentication() {
        // SecurityContext has no authentication
        when(s3Client.listObjectsV2Paginator(any(ListObjectsV2Request.class)))
                .thenReturn(paginatorWith("employee/uuid/doc.pdf"));

        String result = tool.countAccessibleDocuments();

        assertThat(result).contains("employee");
        verify(s3Client, times(1)).listObjectsV2Paginator(any(ListObjectsV2Request.class));
    }

    @Test
    void countAccessibleDocuments_picksHighestPrivilegeRoleWhenUserHasMultiple() {
        setJwtRole("employee", "executive", "manager");
        when(s3Client.listObjectsV2Paginator(any(ListObjectsV2Request.class)))
                .thenReturn(paginatorWith())
                .thenReturn(paginatorWith())
                .thenReturn(paginatorWith())
                .thenReturn(paginatorWith());

        String result = tool.countAccessibleDocuments();

        // executive has 4 prefixes — verify executive was resolved
        assertThat(result).contains("executive");
        verify(s3Client, times(4)).listObjectsV2Paginator(any(ListObjectsV2Request.class));
    }

    @ParameterizedTest(name = "jwtRole={0} → expected in result={1}")
    @CsvSource({"hr,hr", "manager,manager"})
    void countAccessibleDocuments_resolvesRoleCorrectly(String jwtRole, String expectedRole) {
        setJwtRole(jwtRole);
        when(s3Client.listObjectsV2Paginator(any(ListObjectsV2Request.class)))
                .thenReturn(paginatorWith())
                .thenReturn(paginatorWith())
                .thenReturn(paginatorWith());

        String result = tool.countAccessibleDocuments();

        assertThat(result).contains(expectedRole);
    }
}
