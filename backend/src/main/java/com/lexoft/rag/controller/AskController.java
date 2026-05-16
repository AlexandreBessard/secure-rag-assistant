package com.lexoft.rag.controller;

import com.lexoft.rag.model.AskRequest;
import com.lexoft.rag.model.AskResponse;
import com.lexoft.rag.service.ChatService;
import com.lexoft.rag.service.EvaluationService;
import org.springframework.ai.evaluation.EvaluationResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AskController {

    private final ChatService chatService;
    private final EvaluationService evaluationService;

    public AskController(ChatService chatService, EvaluationService evaluationService) {
        this.chatService = chatService;
        this.evaluationService = evaluationService;
    }

    @PostMapping(path = "/ask", produces = "application/json")
    public AskResponse ask(@RequestBody AskRequest request) {
        String answer = chatService.ask(request.question());
        EvaluationResponse evaluation = evaluationService.evaluate(request.question(), answer);
        return new AskResponse(answer, evaluation.isPass(), evaluation.getScore(), evaluation.getFeedback());
    }
}
