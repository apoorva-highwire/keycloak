package org.highwire.iam.config;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.highwire.authsdk.logging.AppLogger;
import org.highwire.authsdk.logging.Loggers;
import org.highwire.iam.web.response.ApiError;
import org.highwire.iam.web.response.ApiResponse;
import org.highwire.iam.web.response.MessageInformationFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Missing/invalid/expired bearer token never reaches a controller — Spring
 * Security rejects it inside its own filter chain, before
 * {@link GlobalExceptionHandler} or any @RestController runs. Without this,
 * that 401 came back with Spring Security's own default body (empty, or a
 * plain WWW-Authenticate challenge depending on version), the one response
 * shape in this service that DIDN'T match {@link ApiResponse} — a caller
 * had to special-case 401 instead of always reading the same envelope.
 *
 * Builds its own ObjectMapper rather than injecting the app-wide bean —
 * confirmed live that autowiring one here fails at startup
 * ("BeanCreationException ... springSecurityFilterChain"): this bean is
 * wired directly into the SecurityFilterChain @Bean method as a
 * constructor argument, which Spring Security's own bean-definition
 * processing resolves before JacksonAutoConfiguration's ObjectMapper bean
 * definition exists. A local instance sidesteps that ordering entirely —
 * this is only ever serializing our own ApiResponse record, nothing that
 * needs the app's global Jackson customization.
 */
@Component
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final AppLogger log = Loggers.of(JsonAuthenticationEntryPoint.class);

    // WRITE_DATES_AS_TIMESTAMPS disabled explicitly — Jackson defaults it
    // to true, but Spring Boot's own auto-configured ObjectMapper turns
    // it off, which is why every other ApiResponse in this service
    // already renders `timestamp` as ISO-8601, not a raw epoch float.
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final MessageInformationFactory messageInformationFactory;

    public JsonAuthenticationEntryPoint(MessageInformationFactory messageInformationFactory) {
        this.messageInformationFactory = messageInformationFactory;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        log.warn("Rejected unauthenticated request to {} {}: {}",
                request.getMethod(), request.getRequestURI(), authException.getMessage());

        ApiResponse<Void> body = ApiResponse.failure(
                "Authentication required",
                List.of(new ApiError(null, "UNAUTHORIZED", authException.getMessage())),
                messageInformationFactory.create());

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), body);
    }
}
