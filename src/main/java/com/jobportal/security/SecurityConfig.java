package com.jobportal.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Map;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final ObjectMapper objectMapper;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, ObjectMapper objectMapper) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    objectMapper.writeValue(response.getOutputStream(),
                            Map.of("error", "Authentication required"));
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    objectMapper.writeValue(response.getOutputStream(),
                            Map.of("error", "Access denied"));
                })
            )
            .authorizeHttpRequests(auth -> auth
                // Auth endpoints — public
                .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
                // Public read access to jobs & companies
                .requestMatchers(HttpMethod.GET, "/api/jobs", "/api/jobs/{id}").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/companies", "/api/companies/{id}").permitAll()
                // Static resources & uploads
                .requestMatchers("/public/**", "/", "/index.html", "/jobs.html",
                        "/apply.html", "/admin.html", "/register.html", "/login.html", "/js/**", "/h2-console/**", "/uploads/**").permitAll()
                // Company management
                .requestMatchers("/api/companies/**").hasAnyRole("RECRUITER", "ADMIN")
                // File upload endpoints
                .requestMatchers(HttpMethod.POST, "/api/upload/resume").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/upload/logo").hasAnyRole("RECRUITER", "ADMIN")
                // Job mutations — RECRUITER or ADMIN only
                .requestMatchers(HttpMethod.POST, "/api/jobs/**").hasAnyRole("RECRUITER", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/jobs/**").hasAnyRole("RECRUITER", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/jobs/**").hasAnyRole("RECRUITER", "ADMIN")
                // Application endpoints
                .requestMatchers(HttpMethod.GET, "/api/applications/me").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/jobs/{jobId}/applications").authenticated()
                .requestMatchers(HttpMethod.PATCH, "/api/applications/{id}/status").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/applications").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/applications").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/applications/{id}").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/applications/{id}").hasRole("ADMIN")
                .requestMatchers("/api/applications/**").authenticated()
                // Everything else — authenticated
                .anyRequest().authenticated()
            )
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}