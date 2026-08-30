package com.pdfFileReader.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * API rotalari icin CORS.
 * - Varsayilan: TUM localhost portlari acik (vite dev 5173, preview 4173,
 *   Live Server 5500 vb.) - yerel gelistirme aracinda origin kaynakli
 *   "CORS istegi basarisiz" hatalarini tamamen ortadan kaldirir.
 * - Ek origin'ler application.properties'ten eklenebilir
 *   (app.cors.allowed-origins, ornek: LAN IP'si).
 */
@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer(
            @Value("${app.cors.allowed-origins:}")
            String[] allowedOrigins
    ) {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                // Tum API rotalarini kapsar: upload, listeleme, upcoming,
                // guncelleme (PUT) ve silme (DELETE) dahil.
                org.springframework.web.servlet.config.annotation.CorsRegistration reg = registry
                        .addMapping("/api/**")
                        .allowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*")
                        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                        .allowedHeaders("*");

                if (allowedOrigins.length > 0 && !allowedOrigins[0].isBlank()) {
                    reg.allowedOrigins(allowedOrigins);
                }
            }
        };
    }
}
