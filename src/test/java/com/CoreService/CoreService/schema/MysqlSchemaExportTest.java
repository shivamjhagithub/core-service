package com.CoreService.CoreService.schema;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the contract between the JPA mappings and the Flyway scripts.
 * <p>
 * The application starts with {@code ddl-auto=validate}, so a table or column
 * that Hibernate expects but no migration creates is a startup failure rather
 * than a runtime one. This test exports the MySQL DDL implied by the entities
 * and compares it against {@code db/migration}, which catches that mismatch
 * without needing a live database.
 */
@DataJpaTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect",
        "spring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false",
        "spring.jpa.properties.hibernate.type.preferred_uuid_jdbc_type=CHAR",
        "spring.jpa.properties.hibernate.type.prefer_native_enum_types=false",
        "spring.jpa.properties.hibernate.format_sql=true",
        "spring.jpa.properties.hibernate.hbm2ddl.delimiter=;",
        "spring.jpa.properties.jakarta.persistence.schema-generation.scripts.action=create",
        "spring.jpa.properties.jakarta.persistence.schema-generation.scripts.create-target="
                + MysqlSchemaExportTest.EXPORTED_DDL
})
class MysqlSchemaExportTest {

    static final String EXPORTED_DDL = "target/generated-mysql-schema.sql";
    private static final Path MIGRATIONS = Path.of("src/main/resources/db/migration");

    static {
        // Hibernate appends to the script rather than replacing it, so without
        // this a run that follows an earlier one would compare against a file
        // holding both generations. Runs before the Spring context is built.
        try {
            Files.deleteIfExists(Path.of(EXPORTED_DDL));
        } catch (IOException ex) {
            throw new IllegalStateException("Could not clear " + EXPORTED_DDL, ex);
        }
    }

    private static final Pattern CREATE_TABLE =
            Pattern.compile("create\\s+table\\s+(?:if\\s+not\\s+exists\\s+)?`?([a-z_0-9]+)`?\\s*\\(",
                    Pattern.CASE_INSENSITIVE);

    /** Constraint clauses that appear where a column definition could. */
    private static final List<String> NOT_A_COLUMN = List.of(
            "primary", "constraint", "unique", "check", "foreign", "key", "index");

    @Test
    @DisplayName("every table and column Hibernate expects is created by a migration")
    void migrationsSatisfyTheMappings() throws IOException {

        Map<String, Map<String, String>> expected = parse(Files.readString(Path.of(EXPORTED_DDL)));
        Map<String, Map<String, String>> migrated = parse(readMigrations());

        assertThat(expected).as("entities should map to tables").isNotEmpty();

        List<String> problems = new ArrayList<>();

        expected.forEach((table, columns) -> {
            Map<String, String> actual = migrated.get(table);
            if (actual == null) {
                problems.add("no migration creates table '" + table + "'");
                return;
            }
            columns.forEach((column, type) -> {
                String actualType = actual.get(column);
                if (actualType == null) {
                    problems.add(table + "." + column + " is missing from the migrations");
                } else if (!actualType.equals(type)) {
                    problems.add(table + "." + column + " is " + actualType
                            + " in the migrations but " + type + " in the mapping");
                }
            });
        });

        assertThat(problems).as("schema drift between JPA mappings and Flyway migrations").isEmpty();
    }

    @Test
    @DisplayName("migrations do not create tables no entity maps")
    void migrationsHaveNoOrphanTables() throws IOException {

        // flyway_schema_history is created by Flyway itself, not by a script.
        TreeSet<String> orphans = new TreeSet<>(parse(readMigrations()).keySet());
        orphans.removeAll(parse(Files.readString(Path.of(EXPORTED_DDL))).keySet());

        assertThat(orphans).isEmpty();
    }

    private String readMigrations() throws IOException {
        try (var files = Files.list(MIGRATIONS)) {
            List<Path> scripts = files.filter(path -> path.getFileName().toString().endsWith(".sql"))
                    .sorted()
                    .toList();

            assertThat(scripts).as("migration scripts").isNotEmpty();

            StringBuilder combined = new StringBuilder();
            for (Path script : scripts) {
                combined.append(Files.readString(script)).append('\n');
            }
            return combined.toString();
        }
    }

    /**
     * @return table name to column name to normalized type
     */
    private Map<String, Map<String, String>> parse(String ddl) {
        String withoutComments = ddl.replaceAll("--[^\\n]*", "");

        Map<String, Map<String, String>> tables = new TreeMap<>();
        Matcher matcher = CREATE_TABLE.matcher(withoutComments);

        while (matcher.find()) {
            String table = matcher.group(1).toLowerCase(Locale.ROOT);
            String body = balancedBody(withoutComments, withoutComments.indexOf('(', matcher.end() - 1));
            tables.put(table, columnsOf(body));
        }
        return tables;
    }

    /** Reads from the opening parenthesis to its match, so nested {@code (6)} does not end the body. */
    private String balancedBody(String ddl, int openParenIndex) {
        int depth = 0;
        for (int i = openParenIndex; i < ddl.length(); i++) {
            char c = ddl.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
                if (depth == 0) {
                    return ddl.substring(openParenIndex + 1, i);
                }
            }
        }
        throw new IllegalStateException("Unbalanced parentheses in DDL near index " + openParenIndex);
    }

    private Map<String, String> columnsOf(String body) {
        Map<String, String> columns = new LinkedHashMap<>();

        for (String definition : splitTopLevel(body)) {
            String trimmed = definition.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            String[] tokens = trimmed.split("\\s+");
            String first = tokens[0].replace("`", "").toLowerCase(Locale.ROOT);
            if (NOT_A_COLUMN.contains(first) || tokens.length < 2) {
                continue;
            }

            columns.put(first, normalizeType(trimmed.substring(tokens[0].length()).trim()));
        }
        return columns;
    }

    private List<String> splitTopLevel(String body) {
        List<String> parts = new ArrayList<>();
        int depth = 0;
        StringBuilder current = new StringBuilder();

        for (char c : body.toCharArray()) {
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            }
            if (c == ',' && depth == 0) {
                parts.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        parts.add(current.toString());
        return parts;
    }

    /**
     * Reduces a column definition to the type Hibernate's validator compares.
     * Length, nullability, defaults and collations are ignored because the
     * validator ignores them too.
     */
    private String normalizeType(String definition) {
        String base = definition.toLowerCase(Locale.ROOT)
                .replaceAll("\\([^)]*\\)", "")
                .replaceAll("\\bnot null\\b", "")
                .replaceAll("\\bnull\\b", "")
                .replaceAll("\\bunique\\b", "")
                .replaceAll("\\bauto_increment\\b", "")
                .replaceAll("\\bdefault\\b.*", "")
                .replaceAll("\\bcomment\\b.*", "")
                .replaceAll("\\bcharacter set\\b.*", "")
                .replaceAll("\\bcollate\\b.*", "")
                .replaceAll("\\bcheck\\b.*", "")
                .replaceAll("\\breferences\\b.*", "")
                .trim();

        String first = base.isEmpty() ? "" : base.split("\\s+")[0];

        return switch (first) {
            case "bool", "tinyint" -> "bit";
            case "int", "int4" -> "integer";
            case "int8" -> "bigint";
            case "float8", "double" -> "float";
            case "timestamp" -> "datetime";
            default -> first;
        };
    }
}
