package com.lexoft.rag.controller;

import com.lexoft.rag.model.AskRequest;
import com.lexoft.rag.model.AskResponse;
import com.lexoft.rag.model.ChatResult;
import com.lexoft.rag.model.HistoryMessage;
import com.lexoft.rag.service.ChatService;
import com.lexoft.rag.service.EvaluationService;
import com.lexoft.rag.service.PromptGuardService;
import org.springframework.ai.evaluation.EvaluationResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class AskController {

    private static final List<String> APP_ROLES = List.of("executive", "hr", "manager", "employee");

    private final ChatService chatService;
    private final EvaluationService evaluationService;
    private final PromptGuardService promptGuardService;

    public AskController(ChatService chatService, EvaluationService evaluationService,
                         PromptGuardService promptGuardService) {
        this.chatService = chatService;
        this.evaluationService = evaluationService;
        this.promptGuardService = promptGuardService;
    }

    @GetMapping(path = "/history", produces = "application/json")
    public List<HistoryMessage> history(@AuthenticationPrincipal Jwt jwt) {
        return chatService.getHistory(jwt.getSubject());
    }

    @DeleteMapping(path = "/history")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearHistory(@AuthenticationPrincipal Jwt jwt) {
        chatService.clearHistory(jwt.getSubject());
    }

    @PostMapping(path = "/ask", produces = "application/json")
    public AskResponse ask(@RequestBody AskRequest request,
                           @AuthenticationPrincipal Jwt jwt) {
        promptGuardService.guard(request.question(), jwt.getSubject());
        String role = extractRole(jwt);
        String conversationId = jwt.getSubject();
        ChatResult result = chatService.ask(request.question(), role, conversationId);
        EvaluationResponse evaluation = evaluationService.evaluate(request.question(), result.answer());
        return new AskResponse(result.answer(), evaluation.isPass(), evaluation.getScore(), evaluation.getFeedback(), result.sources());
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
