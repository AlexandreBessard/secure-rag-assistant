package com.lexoft.rag.controller;

import com.lexoft.rag.model.AskRequest;
import com.lexoft.rag.model.AskResponse;
import com.lexoft.rag.service.ChatService;
import com.lexoft.rag.service.EvaluationService;
import org.springframework.ai.evaluation.EvaluationResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class AskController {

    private static final List<String> APP_ROLES = List.of("executive", "hr", "manager", "employee");

    private final ChatService chatService;
    private final EvaluationService evaluationService;

    public AskController(ChatService chatService, EvaluationService evaluationService) {
        this.chatService = chatService;
        this.evaluationService = evaluationService;
    }

    @PostMapping(path = "/ask", produces = "application/json")
    public AskResponse ask(@RequestBody AskRequest request,
                           @AuthenticationPrincipal Jwt jwt) {
        String role = extractRole(jwt);
        String conversationId = jwt.getSubject();
        String answer = chatService.ask(request.question(), role, conversationId);
        EvaluationResponse evaluation = evaluationService.evaluate(request.question(), answer);
        return new AskResponse(answer, evaluation.isPass(), evaluation.getScore(), evaluation.getFeedback());
    }

    private String extractRole(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null) return "employee";
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) realmAccess.get("roles");
        if (roles == null) return "employee";
        return roles.stream()
                .filter(APP_ROLES::contains)
                .findFirst()
                .orElse("employee");
    }
}
