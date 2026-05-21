package com.lexoft.rag.common.security;

import java.util.Optional;

public enum Role {

    EXECUTIVE("executive"),
    HR("hr"),
    MANAGER("manager"),
    EMPLOYEE("employee");

    private final String value;

    Role(String value) {
        this.value = value;
    }

    /** The string representation used in JWT claims and pgvector metadata. */
    public String value() {
        return value;
    }

    /** Parses a JWT/metadata string to a Role, or empty if unrecognised. */
    public static Optional<Role> fromString(String s) {
        if (s == null) return Optional.empty();
        for (Role r : values()) {
            if (r.value.equalsIgnoreCase(s)) return Optional.of(r);
        }
        return Optional.empty();
    }

    @Override
    public String toString() {
        return value;
    }
}
