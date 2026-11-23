package com.bioinformatics.geneservice.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
class FreshDatabaseMigrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private final JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource(
            POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));

    @Test
    void allServicesInstallFromV1AndRetainTheirOwnHistories() {
        migrate("gene-service", "public", "migration");

        assertThat(jdbc.queryForList("""
                SELECT table_name FROM information_schema.tables
                WHERE table_schema = 'public' AND table_type = 'BASE TABLE'
                """, String.class)).containsExactlyInAnyOrder(
                "flyway_schema_history", "keyword", "go_term", "protein_entry", "protein_keyword",
                "protein_go_term", "cross_reference", "protein_feature", "host_organism",
                "protein_comment", "protein_publication", "saved_filter");
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM pg_sequences WHERE schemaname = 'public' AND increment_by = 500
                """, Integer.class)).isEqualTo(8);

        jdbc.update("""
                INSERT INTO public.protein_entry
                    (accession, entry_name, organism_name, taxid, length, evidence_level)
                VALUES ('P12345', 'TEST_HUMAN', 'Homo sapiens', 9606, 100, 1)
                """);
        assertThat(jdbc.queryForObject("""
                SELECT public.fts_match(search_vector, 'P12345') FROM public.protein_entry
                """, Boolean.class)).isTrue();

        migrate("auth-service", "auth", "db", "migration");
        migrate("import-service", "import_batch", "db", "migration");
        migrate("analytics-service", "analytics", "db", "migration");
        migrate("export-service", "export_data", "db", "migration");
        migrate("audit-service", "audit_log", "migration");

        assertThat(jdbc.queryForObject("""
                SELECT total_proteins FROM analytics.mv_dashboard_kpis
                """, Long.class)).isEqualTo(1);
        jdbc.update("""
                INSERT INTO export_data.export_pipeline (user_id, name, filter_json, format, field_schema)
                VALUES ('test', 'Custom export', '{}', 'CUSTOM_LONG_FORMAT', '[]')
                """);
        jdbc.update("""
                INSERT INTO audit_log.audit_event (event_id, source_service, action)
                VALUES (gen_random_uuid(), 'gene-service', 'AUDIT_SEARCH')
                """);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_log.audit_event", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT increment_by FROM pg_sequences
                WHERE schemaname = 'audit_log' AND sequencename = 'audit_log_seq'
                """, Long.class)).isEqualTo(500);
    }

    @Test
    void refusesToBaselineAnExistingNonemptySchema() {
        jdbc.execute("CREATE SCHEMA unmanaged");
        jdbc.execute("CREATE TABLE unmanaged.existing_record (id integer PRIMARY KEY)");

        assertThatThrownBy(() -> flyway("gene-service", "unmanaged", "migration").migrate())
                .isInstanceOf(FlywayException.class)
                .hasMessageContaining("non-empty schema");
        assertThat(jdbc.queryForObject("SELECT to_regclass('unmanaged.existing_record')::text", String.class))
                .isEqualTo("unmanaged.existing_record");
    }

    private void migrate(String service, String schema, String... location) {
        var flyway = flyway(service, schema, location);
        assertThat(flyway.migrate().migrationsExecuted).as(service).isEqualTo(1);
        assertThat(flyway.info().current().getVersion().getVersion()).as(service).isEqualTo("1");
        assertThat(flyway.migrate().migrationsExecuted).as(service + " restart").isZero();
    }

    private Flyway flyway(String service, String schema, String... location) {
        // Test the sibling service sources in the same shared-database layout used in development.
        var directory = Path.of(System.getProperty("basedir")).resolveSibling(service)
                .resolve("src").resolve("main").resolve("resources");
        for (var segment : location) {
            directory = directory.resolve(segment);
        }
        return Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .defaultSchema(schema)
                .locations("filesystem:" + directory)
                .failOnMissingLocations(true)
                .baselineOnMigrate(false)
                .cleanDisabled(true)
                .load();
    }
}
