package com.lexoft.rag.ingestion.exception;

/**
 * Thrown when a document cannot be parsed — corrupt file, password-protected, unsupported encoding, etc.
 * This is a permanent failure: retrying the same file will produce the same result.
 */
public class DocumentParseException extends RuntimeException {

    public DocumentParseException(String documentId, String filename, Throwable cause) {
        super("Failed to parse document '" + filename + "' (id=" + documentId + ")", cause);
    }
}
