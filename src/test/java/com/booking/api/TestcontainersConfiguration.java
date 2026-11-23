package com.booking.api;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer("postgres:16-alpine")
                .withEnv("TZ", "UTC")
                .withEnv("PGTZ", "UTC")
                .withCopyFileToContainer(MountableFile.forClasspathResource("schema.sql"),
                        "/docker-entrypoint-initdb.d/01-schema.sql");
    }
}
