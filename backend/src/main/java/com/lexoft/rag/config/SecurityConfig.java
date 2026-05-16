package com.lexoft.rag.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                // Delegate preflight CORS handling to WebConfig (Spring MVC CorsConfigurer)
                // Customizer.withDefaults() tells Spring Security to look for a CORS configuration source in the Spring MVC context — which it finds in
                //  WebConfig.addCorsMappings()
                .cors(Customizer.withDefaults())
                // Stateless REST API — no CSRF token needed
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health").permitAll()
                        .anyRequest().authenticated()
                )
                // Validate Bearer JWTs against Keycloak's JWKS (issuer-uri auto-discovers the endpoint)
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .build();
    }
}
