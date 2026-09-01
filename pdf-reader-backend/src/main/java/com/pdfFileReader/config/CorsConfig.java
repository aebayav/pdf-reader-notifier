package com.pdfFileReader.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

/**
 * Global CORS filtresi - filtre zincirinin EN BASINDA calisir. Bu sayede
 * AuthFilter'in kisa devre 401 yanitlarina bile Access-Control-Allow-Origin
 * eklenir (MVC katmani filtreden sonra geldigi icin tek basina yetmezdi).
 * - Varsayilan: TUM localhost portlari acik (vite dev 5173, preview 4173,
 *   Live Server 5500 vb.)
 * - Ek origin'ler application.properties'ten eklenebilir
 *   (app.cors.allowed-origins, ornek: LAN IP'si)
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter(
            @Value("${app.cors.allowed-origins:}")
            String[] allowedOrigins
    ) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("http://localhost:*", "http://127.0.0.1:*"));
        if (allowedOrigins.length > 0 && !allowedOrigins[0].isBlank()) {
            config.setAllowedOrigins(List.of(allowedOrigins));
        }
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setMaxAge(1800L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);

        return new CorsFilter(source);
    }

    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilterRegistration(CorsFilter corsFilter) {
        FilterRegistrationBean<CorsFilter> registration = new FilterRegistrationBean<>(corsFilter);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
