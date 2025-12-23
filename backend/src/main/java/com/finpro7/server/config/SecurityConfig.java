package com.finpro7.server.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())

            // kalo pake JWT/token, ini cocok ga bikin session
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .authorizeHttpRequests(auth -> auth
                // preflight browser (CORS)
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // auth tetap publik
                .requestMatchers("/auth/**").permitAll()

                // health check publik
                .requestMatchers("/", "/test", "/ping").permitAll()

                // actuator health saja
                .requestMatchers("/actuator/health").permitAll()

                // API game sementara dibuka dulu biar web build bisa jalan
                // nanti kalau sudah siap auth token, kunci lagi
                .requestMatchers("/api/**").permitAll()

                // sisanya butuh login
                .anyRequest().authenticated()
            );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();

        cfg.setAllowedOriginPatterns(List.of(
            // test lokal html
            "http://localhost:*",
            "http://127.0.0.1:*",

            // itch
            "https://itch.io",
            "https://*.itch.io",
            "https://*.itch.zone"
        ));

        cfg.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));

        // kalau ga pakai cookie session dari browser, biarin false
        cfg.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
