package com.erp.ia.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

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
            // Production: require authentication
            http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/swagger-ui/**", "/api-docs/**", "/actuator/**").permitAll()
                            .requestMatchers("/api/v1/**").authenticated()
                            .anyRequest().permitAll());
            // JWT filter would be added here in production
        }

        return http.build();
    }
}
