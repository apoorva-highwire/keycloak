package org.highwire.iam.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI iamServiceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("HighWire IAM Service")
                .version("0.1.0")
                .description("Thin orchestrator over Keycloak's Admin REST API — "
                        + "provisions publisher realms and manages journal entitlements. "
                        + "Stateless: Keycloak itself is the only store."));
    }
}
