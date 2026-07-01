package com.languageschool.backend.testsupport;

import org.testcontainers.containers.PostgreSQLContainer;

public final class TestPostgresContainer {

    private static final PostgreSQLContainer<?> CONTAINER;

    static {
        CONTAINER = new PostgreSQLContainer<>("postgres:18")
                .withDatabaseName("language_application_test")
                .withUsername("postgres")
                .withPassword("postgres")
                .withInitScript("testcontainers/01_extensions.sql");
        CONTAINER.start();
    }

    private TestPostgresContainer() {
    }

    public static PostgreSQLContainer<?> get() {
        return CONTAINER;
    }
}
