CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS SPRING_AI_CHAT_MEMORY (
    conversation_id VARCHAR(36) NOT NULL,
    content         TEXT        NOT NULL,
    type            VARCHAR(10) NOT NULL CHECK (type IN ('USER', 'ASSISTANT', 'SYSTEM', 'TOOL')),
    "timestamp"     TIMESTAMP   NOT NULL
);

CREATE INDEX IF NOT EXISTS SPRING_AI_CHAT_MEMORY_CONVERSATION_ID_TIMESTAMP_IDX
    ON SPRING_AI_CHAT_MEMORY (conversation_id, "timestamp");

CREATE TABLE IF NOT EXISTS RAG_SOURCES (
    id              BIGSERIAL PRIMARY KEY,
    conversation_id VARCHAR(36) NOT NULL,
    message_text    TEXT        NOT NULL,
    document_name   TEXT        NOT NULL,
    document_id     TEXT        NOT NULL
);

CREATE INDEX IF NOT EXISTS RAG_SOURCES_CONVERSATION_ID_IDX
    ON RAG_SOURCES (conversation_id);
