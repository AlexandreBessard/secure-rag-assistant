package com.lexoft.rag.model;

import java.util.List;

public record ChatResult(String answer, List<Source> sources) {}
