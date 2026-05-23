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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DocumentUploadServiceTest {

    // %PDF magic bytes padded to 8 bytes to satisfy the header read
    private static final byte[] PDF_BYTES = {0x25, 0x50, 0x44, 0x46, 0, 0, 0, 0};

    @Mock
    private S3Client s3Client;

    private DocumentUploadService service;

    @BeforeEach
    void setUp() {
        service = new DocumentUploadService(s3Client, "test-bucket", 20 * 1024 * 1024L);
    }

    @ParameterizedTest(name = "role={0}")
    @ValueSource(strings = {"executive", "hr", "manager", "employee"})
    void upload_acceptsAllValidRoles(String role) {
        assertThatNoException().isThrownBy(
                () -> service.upload(role, "report.pdf", PDF_BYTES.length,
                        new ByteArrayInputStream(PDF_BYTES)));
    }

    @Test
    void upload_throwsIllegalArgumentForUnknownRole() throws IOException {
        assertThatThrownBy(() -> service.upload("admin", "file.txt", 1L,
                new ByteArrayInputStream(new byte[]{1})))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown role: admin");
    }

    @Test
    void upload_throwsForEmptyRoleString() throws IOException {
        assertThatThrownBy(() -> service.upload("", "file.txt", 1L,
                new ByteArrayInputStream(new byte[]{1})))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void upload_s3KeyStartsWithTargetRolePrefix() throws IOException {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Consumer<PutObjectRequest.Builder>> captor =
                ArgumentCaptor.forClass(Consumer.class);

        service.upload("manager", "report.pdf", PDF_BYTES.length,
                new ByteArrayInputStream(PDF_BYTES));

        verify(s3Client).putObject(captor.capture(), any(RequestBody.class));

        PutObjectRequest.Builder builder = PutObjectRequest.builder();
        captor.getValue().accept(builder);
        PutObjectRequest request = builder.build();

        assertThat(request.key()).startsWith("manager/");
        assertThat(request.key()).endsWith("/report.pdf");
    }

    @Test
    void upload_s3KeyContainsUuidSegment() throws IOException {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Consumer<PutObjectRequest.Builder>> captor =
                ArgumentCaptor.forClass(Consumer.class);

        service.upload("employee", "doc.pdf", PDF_BYTES.length,
                new ByteArrayInputStream(PDF_BYTES));

        verify(s3Client).putObject(captor.capture(), any(RequestBody.class));

        PutObjectRequest.Builder builder = PutObjectRequest.builder();
        captor.getValue().accept(builder);
        String key = builder.build().key();

        // key format: role/UUID/filename — three segments
        String[] segments = key.split("/");
        assertThat(segments).hasSize(3);
        assertThat(segments[0]).isEqualTo("employee");
        assertThat(segments[1]).hasSizeGreaterThan(10);
        assertThat(segments[2]).isEqualTo("doc.pdf");
    }

    @Test
    void upload_usesBucketFromConfiguration() throws IOException {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Consumer<PutObjectRequest.Builder>> captor =
                ArgumentCaptor.forClass(Consumer.class);

        service.upload("hr", "policy.pdf", PDF_BYTES.length,
                new ByteArrayInputStream(PDF_BYTES));

        verify(s3Client).putObject(captor.capture(), any(RequestBody.class));

        PutObjectRequest.Builder builder = PutObjectRequest.builder();
        captor.getValue().accept(builder);
        assertThat(builder.build().bucket()).isEqualTo("test-bucket");
    }

    @Test
    void upload_returnsS3KeyMatchingActualUploadPath() throws IOException {
        String returnedKey = service.upload("executive", "brief.pdf", PDF_BYTES.length,
                new ByteArrayInputStream(PDF_BYTES));
        assertThat(returnedKey).startsWith("executive/");
        assertThat(returnedKey).endsWith("/brief.pdf");
    }
}
