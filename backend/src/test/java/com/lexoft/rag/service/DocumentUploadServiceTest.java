package com.lexoft.rag.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DocumentUploadServiceTest {

    @Mock
    private S3Client s3Client;

    private DocumentUploadService service;

    @BeforeEach
    void setUp() {
        service = new DocumentUploadService(s3Client, "test-bucket");
    }

    @ParameterizedTest(name = "role={0}")
    @ValueSource(strings = {"executive", "hr", "manager", "employee"})
    void upload_acceptsAllValidRoles(String role) {
        assertThatNoException().isThrownBy(
                () -> service.upload(role, "report.pdf", new byte[]{1, 2, 3}));
    }

    @Test
    void upload_throwsIllegalArgumentForUnknownRole() {
        assertThatThrownBy(() -> service.upload("admin", "file.pdf", new byte[]{1}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown role: admin");
    }

    @Test
    void upload_throwsForEmptyRoleString() {
        assertThatThrownBy(() -> service.upload("", "file.pdf", new byte[]{1}))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void upload_s3KeyStartsWithTargetRolePrefix() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Consumer<PutObjectRequest.Builder>> captor =
                ArgumentCaptor.forClass(Consumer.class);

        service.upload("manager", "report.pdf", new byte[]{1, 2, 3});

        verify(s3Client).putObject(captor.capture(), any(RequestBody.class));

        PutObjectRequest.Builder builder = PutObjectRequest.builder();
        captor.getValue().accept(builder);
        PutObjectRequest request = builder.build();

        assertThat(request.key()).startsWith("manager/");
        assertThat(request.key()).endsWith("/report.pdf");
    }

    @Test
    void upload_s3KeyContainsUuidSegment() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Consumer<PutObjectRequest.Builder>> captor =
                ArgumentCaptor.forClass(Consumer.class);

        service.upload("employee", "doc.pdf", new byte[]{1});

        verify(s3Client).putObject(captor.capture(), any(RequestBody.class));

        PutObjectRequest.Builder builder = PutObjectRequest.builder();
        captor.getValue().accept(builder);
        String key = builder.build().key();

        // key format: role/UUID/filename — three segments
        String[] segments = key.split("/");
        assertThat(segments).hasSize(3);
        assertThat(segments[0]).isEqualTo("employee");
        // UUID segment — basic length check
        assertThat(segments[1]).hasSizeGreaterThan(10);
        assertThat(segments[2]).isEqualTo("doc.pdf");
    }

    @Test
    void upload_usesBucketFromConfiguration() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Consumer<PutObjectRequest.Builder>> captor =
                ArgumentCaptor.forClass(Consumer.class);

        service.upload("hr", "policy.pdf", new byte[]{1});

        verify(s3Client).putObject(captor.capture(), any(RequestBody.class));

        PutObjectRequest.Builder builder = PutObjectRequest.builder();
        captor.getValue().accept(builder);
        assertThat(builder.build().bucket()).isEqualTo("test-bucket");
    }

    @Test
    void upload_returnsS3KeyMatchingActualUploadPath() {
        String returnedKey = service.upload("executive", "brief.pdf", new byte[]{1, 2});
        assertThat(returnedKey).startsWith("executive/");
        assertThat(returnedKey).endsWith("/brief.pdf");
    }
}
