package com.erp.ia.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration.
 *
 * MVP STATUS: JWT is NOT implemented yet.
 * - security.enabled=false (dev/test): all endpoints are open
 * - security.enabled=true (production): endpoints require authentication
 * via Spring's default basic auth (in-memory UserDetailsService).
 *
 * TODO: Replace with a real JwtAuthenticationFilter for production.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${security.enabled:false}")
    private boolean securityEnabled;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable());

        if (!securityEnabled) {
            // Dev/test: allow all
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        } else {
            // Production: require authentication (currently basic auth, NOT JWT)
            // TODO: Add JwtAuthenticationFilter before UsernamePasswordAuthenticationFilter
            http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/swagger-ui/**", "/api-docs/**", "/actuator/**").permitAll()
                            .requestMatchers("/api/v1/**").authenticated()
                            .anyRequest().permitAll());
        }

        return http.build();
    }
}
