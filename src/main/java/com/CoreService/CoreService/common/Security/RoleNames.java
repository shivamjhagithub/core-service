package com.CoreService.CoreService.common.security;

/**
 * Role names that carry platform-level meaning. College administrators may
 * define additional roles per tenant; only these are referenced from code.
 */
public final class RoleNames {

    /** Platform operator. Not bound to any college. */
    public static final String MAIN_ADMIN = "MAIN_ADMIN";
    public static final String COLLEGE_ADMIN = "COLLEGE_ADMIN";
    public static final String TEACHER = "TEACHER";
    public static final String STUDENT = "STUDENT";

    private RoleNames() {
    }
}
