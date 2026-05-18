package com.lexoft.rag.model;

import java.util.List;

public record HistoryMessage(String role, String text, List<Source> sources) {}
