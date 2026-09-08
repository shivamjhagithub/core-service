package com.CoreService.CoreService.student.service;

import java.util.UUID;

/**
 * Student module's published API. People are referenced across modules by
 * {@code userId} (the users table key), so only modules that need the academic
 * student record itself go through here.
 */
public interface StudentLookupService {

    boolean isStudent(String userId);

    /** @return the student record id for a user, or null when not a student. */
    UUID studentIdOf(String userId);
}
