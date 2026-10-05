package org.highwire.iam.web.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

/** decisionStrategy is always AFFIRMATIVE — see KeycloakGateway.createScopePermission's javadoc. */
public record CreateScopePermissionRequest(
        @NotBlank String name,
        @NotBlank String resourceName,
        @NotEmpty List<String> scopeNames,
        @NotEmpty List<String> policyNames) {
}
