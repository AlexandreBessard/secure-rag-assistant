package com.lexoft.rag.service;

import com.lexoft.rag.exception.PromptBlockedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/*
Suspicious patterns in the input (ignore your instructions, jailbreak etc...)
Before the LLM is called - zero cost
 */
@Service
public class PromptGuardService {

    private static final Logger log = LoggerFactory.getLogger(PromptGuardService.class);

    private static final List<Pattern> INJECTION_PATTERNS = List.of(
            Pattern.compile("ignore\\s+(your|all|previous|the)\\s+instructions", Pattern.CASE_INSENSITIVE),
            Pattern.compile("forget\\s+(your|the|all)\\s+(instructions|prompt|rules)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("pretend\\s+(you\\s+are|to\\s+be|you're)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("act\\s+as\\s+(if|a|an)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("you\\s+are\\s+now\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("override\\s+(your|the)\\s+(instructions|system|constraints)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("reveal\\s+(your|the)\\s+(system|prompt|instructions)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bjailbreak\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bDAN\\b"),
            Pattern.compile("developer\\s+mode", Pattern.CASE_INSENSITIVE),
            Pattern.compile("unrestricted\\s+mode", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bbypass\\b", Pattern.CASE_INSENSITIVE)
    );

    private final int maxLength;
    private final List<String> blockedTerms;
    private final ComprehendModerationService comprehendModerationService;

    public PromptGuardService(
            @Value("${app.guard.max-length:2000}") int maxLength,
            @Value("${app.guard.blocked-terms:}") String blockedTermsRaw,
            ComprehendModerationService comprehendModerationService) {
        this.maxLength = maxLength;
        this.blockedTerms = Arrays.stream(blockedTermsRaw.split(","))
                .map(String::trim)
                .filter(t -> !t.isEmpty())
                .toList();
        this.comprehendModerationService = comprehendModerationService;
    }

    public void guard(String question, String userId) {
        if (question == null || question.isBlank()) {
            throw new PromptBlockedException("Question must not be blank");
        }
        if (question.length() > maxLength) {
            throw new PromptBlockedException("Question exceeds maximum allowed length of " + maxLength + " characters");
        }

        String lower = question.toLowerCase();

        // Layer 1 — zero-cost regex check for injection/jailbreak patterns
        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(question).find()) {
                log.warn("Prompt injection blocked — user={} pattern='{}' excerpt='{}'",
                        userId, pattern.pattern(), excerpt(question));
                throw new PromptBlockedException("Prompt injection detected");
            }
        }

        // Layer 2 — zero-cost keyword blocklist for custom business terms
        for (String term : blockedTerms) {
            if (lower.contains(term.toLowerCase())) {
                log.warn("Blocked term matched — user={} term='{}' excerpt='{}'",
                        userId, term, excerpt(question));
                throw new PromptBlockedException("Message contains a blocked term");
            }
        }

        // Layer 3 — Amazon Comprehend ML-based toxic content detection
        comprehendModerationService.moderate(question, userId);
    }

    private static String excerpt(String text) {
        return text.length() <= 80 ? text : text.substring(0, 80) + "...";
    }
}
