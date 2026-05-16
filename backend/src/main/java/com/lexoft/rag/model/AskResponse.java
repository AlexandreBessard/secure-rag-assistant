package com.lexoft.rag.model;

public record AskResponse(
        String answer,
        boolean relevant,
        float score,
        String feedback
) {}
