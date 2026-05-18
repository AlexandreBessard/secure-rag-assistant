package com.lexoft.rag.model;

import java.util.List;

public record AskResponse(
        String answer,
        boolean relevant,
        float score,
        String feedback,
        List<Source> sources
) {}
