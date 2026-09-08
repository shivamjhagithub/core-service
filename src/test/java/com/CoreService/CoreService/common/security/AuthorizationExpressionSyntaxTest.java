package com.CoreService.CoreService.common.security;

import com.CoreService.CoreService.CoreServiceApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Parses every {@code @PreAuthorize} / {@code @PostAuthorize} expression in the
 * application.
 * <p>
 * Spring Security compiles these lazily, on the first call to the guarded
 * method, so a malformed expression is not a startup failure — it is a 500 the
 * first time someone hits that endpoint. Parsing them here turns that into a
 * build failure instead.
 */
class AuthorizationExpressionSyntaxTest {

    private static final String BASE_PACKAGE = CoreServiceApplication.class.getPackageName();

    private final SpelExpressionParser parser = new SpelExpressionParser();

    @Test
    @DisplayName("every authorization expression is valid SpEL")
    void authorizationExpressionsParse() {

        List<String> failures = new ArrayList<>();
        int parsed = 0;

        for (Class<?> type : applicationClasses()) {
            for (String expression : expressionsOn(type)) {
                parsed++;
                try {
                    parser.parseExpression(expression);
                } catch (ParseException ex) {
                    failures.add(type.getSimpleName() + ": \"" + expression + "\" -> " + ex.getMessage());
                }
            }
        }

        assertThat(parsed).as("authorization expressions found to check").isPositive();
        assertThat(failures).as("malformed authorization expressions").isEmpty();
    }

    private List<String> expressionsOn(Class<?> type) {
        List<String> expressions = new ArrayList<>();

        PreAuthorize onType = type.getAnnotation(PreAuthorize.class);
        if (onType != null) {
            expressions.add(onType.value());
        }

        for (Method method : type.getDeclaredMethods()) {
            PreAuthorize pre = method.getAnnotation(PreAuthorize.class);
            if (pre != null) {
                expressions.add(pre.value());
            }
            PostAuthorize post = method.getAnnotation(PostAuthorize.class);
            if (post != null) {
                expressions.add(post.value());
            }
        }
        return expressions;
    }

    private List<Class<?>> applicationClasses() {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter((metadataReader, factory) -> true);

        List<Class<?>> classes = new ArrayList<>();
        for (var candidate : scanner.findCandidateComponents(BASE_PACKAGE)) {
            String name = candidate.getBeanClassName();
            if (name == null) {
                continue;
            }
            try {
                // Loaded without initialization: this test inspects annotations
                // and must not run any static setup.
                classes.add(Class.forName(name, false, getClass().getClassLoader()));
            } catch (ClassNotFoundException | LinkageError ignored) {
                // Not loadable in isolation; nothing to inspect.
            }
        }

        assertThat(classes).as("classes scanned under " + BASE_PACKAGE).isNotEmpty();
        return classes;
    }
}
