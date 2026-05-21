package com.lexoft.rag.exception;

public class PromptBlockedException extends RuntimeException {

    public PromptBlockedException(String reason) {
        super(reason);
    }
}
