package com.lexoft.rag.service;

import com.lexoft.rag.exception.PromptBlockedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.comprehend.ComprehendClient;
import software.amazon.awssdk.services.comprehend.model.DetectToxicContentRequest;
import software.amazon.awssdk.services.comprehend.model.DetectToxicContentResponse;
import software.amazon.awssdk.services.comprehend.model.ToxicContent;
import software.amazon.awssdk.services.comprehend.model.ToxicLabels;
import software.amazon.awssdk.services.comprehend.model.ToxicContentType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComprehendModerationServiceTest {

    @Mock
    private ComprehendClient comprehendClient;

    private ComprehendModerationService service(float threshold) {
        return new ComprehendModerationService(comprehendClient, threshold);
    }

    private void stubToxicity(float score) {
        ToxicLabels labels = ToxicLabels.builder()
                .toxicity(score)
                .labels(List.of())
                .build();
        DetectToxicContentResponse response = DetectToxicContentResponse.builder()
                .resultList(labels)
                .build();
        when(comprehendClient.detectToxicContent(any(DetectToxicContentRequest.class)))
                .thenReturn(response);
    }

    private void stubToxicityWithCategory(float score, ToxicContentType category) {
        ToxicContent label = ToxicContent.builder()
                .name(category)
                .score(score)
                .build();
        ToxicLabels labels = ToxicLabels.builder()
                .toxicity(score)
                .labels(label)
                .build();
        DetectToxicContentResponse response = DetectToxicContentResponse.builder()
                .resultList(labels)
                .build();
        when(comprehendClient.detectToxicContent(any(DetectToxicContentRequest.class)))
                .thenReturn(response);
    }

    @Test
    void moderate_passesWhenScoreBelowThreshold() {
        stubToxicity(0.3f);
        assertThatNoException().isThrownBy(() -> service(0.7f).moderate("normal text", "user-1"));
    }

    @Test
    void moderate_passesJustBelowThreshold() {
        stubToxicity(0.699f);
        assertThatNoException().isThrownBy(() -> service(0.7f).moderate("borderline text", "user-1"));
    }

    @Test
    void moderate_blocksAtExactThreshold() {
        stubToxicity(0.7f);
        assertThatThrownBy(() -> service(0.7f).moderate("toxic text", "user-1"))
                .isInstanceOf(PromptBlockedException.class)
                .hasMessageContaining("inappropriate content");
    }

    @Test
    void moderate_blocksAboveThreshold() {
        stubToxicity(0.95f);
        assertThatThrownBy(() -> service(0.7f).moderate("very toxic text", "user-1"))
                .isInstanceOf(PromptBlockedException.class);
    }

    @Test
    void moderate_includesCategoryInBlockedMessage() {
        stubToxicityWithCategory(0.9f, ToxicContentType.HATE_SPEECH);
        assertThatThrownBy(() -> service(0.7f).moderate("hateful text", "user-1"))
                .isInstanceOf(PromptBlockedException.class)
                .hasMessageContaining("HATE_SPEECH");
    }

    @Test
    void moderate_skipsGracefullyWhenComprehendIsUnavailable() {
        when(comprehendClient.detectToxicContent(any(DetectToxicContentRequest.class)))
                .thenThrow(new RuntimeException("Comprehend endpoint unreachable"));
        // Graceful degradation — must not throw
        assertThatNoException().isThrownBy(() -> service(0.7f).moderate("some text", "user-1"));
    }

    @Test
    void moderate_skipsGracefullyOnEmptyResultList() {
        DetectToxicContentResponse emptyResponse = DetectToxicContentResponse.builder()
                .resultList(List.of())
                .build();
        when(comprehendClient.detectToxicContent(any(DetectToxicContentRequest.class)))
                .thenReturn(emptyResponse);
        // Empty result list causes IndexOutOfBoundsException — must degrade gracefully
        assertThatNoException().isThrownBy(() -> service(0.7f).moderate("some text", "user-1"));
    }
}
