package org.highwire.iam.config;

import org.highwire.authsdk.caller.CallerResolvingFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Token validation itself (local JWT verification against the right
 * realm's own signing key, resolved per-request from the token's own
 * `iss`) lives in auth-sdk's JwtAuthConfig — this is only what's
 * specific to iam-service: which endpoints require authentication.
 * @EnableWebSecurity is required explicitly since this project doesn't
 * pull in the spring-boot-starter-security umbrella starter, only the
 * individual modules via auth-sdk.
 *
 * CallerResolvingFilter is wired in via addFilterAfter, not left to
 * global servlet-filter ordering — confirmed live that this app's
 * security filter chain doesn't register at the usual -100 without the
 * full starter, so no numeric FilterRegistrationBean order can
 * guarantee running after it. Inserting it directly into Spring
 * Security's own chain, right after the filter that actually sets the
 * Authentication, sidesteps that entirely. Every consumer of auth-sdk
 * needs this same one line in its own SecurityConfig.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http, JwtDecoder jwtDecoder, CallerResolvingFilter callerResolvingFilter,
            JsonAuthenticationEntryPoint jsonAuthenticationEntryPoint) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize
                        // prometheus: the prometheus service scrapes this with no
                        // bearer token at all — there's no way for it to carry one.
                        .requestMatchers("/actuator/health", "/actuator/prometheus", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.decoder(jwtDecoder))
                        // Default behavior returns Spring Security's own 401 body,
                        // not the ApiResponse envelope every other endpoint in this
                        // service returns — this makes a rejected/missing/expired
                        // token's response shape consistent with everything else.
                        .authenticationEntryPoint(jsonAuthenticationEntryPoint))
                .addFilterAfter(callerResolvingFilter, BearerTokenAuthenticationFilter.class);
        return http.build();
    }
}
