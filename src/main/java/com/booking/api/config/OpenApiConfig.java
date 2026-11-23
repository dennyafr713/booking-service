package com.booking.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import tools.jackson.databind.ObjectMapper;

/**
 * Konfigurasi dokumentasi OpenAPI / Swagger UI.
 * <p>
 * Swagger UI : {@code http://localhost:8080/api/v1/swagger-ui.html}
 * OpenAPI    : {@code http://localhost:8080/api/v1/v3/api-docs}
 * <p>
 * Semua endpoint secara default membutuhkan Bearer JWT. Endpoint publik
 * (mis. login) menimpa aturan ini dengan {@code @SecurityRequirements}.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Booking Service API",
                version = "v1",
                description = "API untuk login, pengelolaan ruangan, dan booking ruangan."),
        security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH))
@SecurityScheme(
        name = OpenApiConfig.BEARER_AUTH,
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Ambil token dari POST /auth/login, lalu tempel di sini (tanpa prefix 'Bearer ').")
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";

    /**
     * Pakai ObjectMapper milik aplikasi supaya schema Swagger mengikuti konfigurasi
     * Jackson yang sama (mis. spring.jackson.property-naming-strategy: SNAKE_CASE).
     * Tanpa ini, Swagger menampilkan nama field camelCase (roomId) padahal API
     * sebenarnya memakai snake_case (room_id).
     */
    @Bean
    public ModelResolver modelResolver(ObjectMapper objectMapper) {
        return new ModelResolver(objectMapper);
    }
}
