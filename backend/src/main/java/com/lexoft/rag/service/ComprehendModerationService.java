package com.lexoft.rag.service;

import com.lexoft.rag.exception.PromptBlockedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.comprehend.ComprehendClient;
import software.amazon.awssdk.services.comprehend.model.DetectToxicContentRequest;
import software.amazon.awssdk.services.comprehend.model.LanguageCode;
import software.amazon.awssdk.services.comprehend.model.TextSegment;
import software.amazon.awssdk.services.comprehend.model.ToxicLabels;

@Service
public class ComprehendModerationService {

    private static final Logger log = LoggerFactory.getLogger(ComprehendModerationService.class);

    private final ComprehendClient comprehendClient;
    private final float toxicityThreshold;

    public ComprehendModerationService(ComprehendClient comprehendClient,
                                       @Value("${app.guard.toxicity-threshold:0.7}") float toxicityThreshold) {
        this.comprehendClient = comprehendClient;
        this.toxicityThreshold = toxicityThreshold;
    }

    public void moderate(String text, String userId) {
        ToxicLabels result;
        try {
            result = comprehendClient.detectToxicContent(
                    DetectToxicContentRequest.builder()
                            .textSegments(TextSegment.builder().text(text).build())
                            .languageCode(LanguageCode.EN)
                            .build()
            ).resultList().get(0);
        } catch (Exception e) {
            log.warn("Comprehend moderation unavailable — skipping toxic content check. user={} error={}", userId, e.getMessage());
            return;
        }

        float score = result.toxicity();
        if (score >= toxicityThreshold) {
            String triggeredCategories = result.labels().stream()
                    .filter(l -> l.score() >= toxicityThreshold)
                    .map(l -> l.name().toString())
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("unspecified");
            log.warn("Toxic content blocked — user={} score={} categories=[{}] excerpt='{}'",
                    userId, score, triggeredCategories, excerpt(text));
            throw new PromptBlockedException("Message contains inappropriate content: " + triggeredCategories);
        }
    }

    private static String excerpt(String text) {
        return text.length() <= 80 ? text : text.substring(0, 80) + "...";
    }
}
