package com.lexoft.rag.ingestion.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.UUID;

@RestControllerAdvice
public class IngestionExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(IngestionExceptionHandler.class);

    @ExceptionHandler(DocumentParseException.class)
    public ProblemDetail handleParseFailure(DocumentParseException e) {
        log.error("Document parse failure: {}", e.getMessage(), e);
        var problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "The file could not be parsed. It may be corrupt, password-protected, or an unsupported format.");
        problem.setTitle("Document Parse Failed");
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception e) {
        String ref = UUID.randomUUID().toString();
        log.error("Unhandled ingestion exception [ref={}]", ref, e);
        var problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Reference: " + ref);
        problem.setTitle("Internal Error");
        return problem;
    }
}
