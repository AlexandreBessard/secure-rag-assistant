package com.lexoft.rag.common.security;

import java.util.List;
import java.util.Map;

public final class RoleHierarchy {

    public static final String EXECUTIVE = "executive";
    public static final String HR        = "hr";
    public static final String MANAGER   = "manager";
    public static final String EMPLOYEE  = "employee";

    /** Fallback when no recognised role is present — least privilege. */
    public static final String DEFAULT = EMPLOYEE;

    /** All recognised roles, ordered highest → lowest privilege. */
    public static final List<String> ALL = List.of(EXECUTIVE, HR, MANAGER, EMPLOYEE);

    /**
     * Role → document tiers visible to that role.
     * executive sees everything; hr/manager see their own tier plus employee; employee sees only employee.
     */
    public static final Map<String, List<String>> ACCESSIBLE = Map.of(
            EXECUTIVE, List.of(EXECUTIVE, MANAGER, HR, EMPLOYEE),
            HR,        List.of(HR, MANAGER, EMPLOYEE),
            MANAGER,   List.of(MANAGER, EMPLOYEE),
            EMPLOYEE,  List.of(EMPLOYEE)
    );

    /**
     * Picks the highest-privilege app role from a raw JWT roles list.
     * Returns {@link #DEFAULT} when the list is null or contains no recognised role.
     */
    public static String resolve(List<String> jwtRoles) {
        if (jwtRoles == null) return DEFAULT;
        return ALL.stream()
                .filter(jwtRoles::contains)
                .findFirst()
                .orElse(DEFAULT);
    }

    private RoleHierarchy() {}
}
