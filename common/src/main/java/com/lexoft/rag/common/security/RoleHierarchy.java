package com.lexoft.rag.common.security;

import java.util.List;
import java.util.Map;

public final class RoleHierarchy {

    /** Fallback when no recognised role is present — least privilege. */
    public static final Role DEFAULT = Role.EMPLOYEE;

    /** All recognised roles, ordered highest → lowest privilege. */
    public static final List<Role> ALL = List.of(Role.EXECUTIVE, Role.HR, Role.MANAGER, Role.EMPLOYEE);

    /**
     * Role → document tiers visible to that role.
     * executive sees everything; hr/manager see their own tier plus employee; employee sees only employee.
     */
    public static final Map<Role, List<Role>> ACCESSIBLE = Map.of(
            Role.EXECUTIVE, List.of(Role.EXECUTIVE, Role.MANAGER, Role.HR, Role.EMPLOYEE),
            Role.HR,        List.of(Role.HR, Role.MANAGER, Role.EMPLOYEE),
            Role.MANAGER,   List.of(Role.MANAGER, Role.EMPLOYEE),
            Role.EMPLOYEE,  List.of(Role.EMPLOYEE)
    );

    /**
     * Picks the highest-privilege app role from a raw JWT roles list.
     * Returns {@link #DEFAULT} when the list is null or contains no recognised role.
     */
    public static Role resolve(List<String> jwtRoles) {
        if (jwtRoles == null) return DEFAULT;
        return ALL.stream()
                .filter(r -> jwtRoles.contains(r.value()))
                .findFirst()
                .orElse(DEFAULT);
    }

    private RoleHierarchy() {}
}
