package org.highwire.iam.web;

import java.util.List;

import org.highwire.iam.gateway.KeycloakGateway;
import org.highwire.iam.gateway.KeycloakGateway.PagedPolicies;
import org.highwire.iam.web.dto.CreateRolePolicyRequest;
import org.highwire.iam.web.dto.PolicyResponse;
import org.highwire.iam.web.response.ApiResponse;
import org.highwire.iam.web.response.MessageInformationFactory;
import org.highwire.iam.web.response.PagedApiResponse;
import org.highwire.iam.web.response.PaginationInfo;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Role-based authorization policies — "the caller must hold at least one
 * of these realm roles."
 */
@RestController
@RequestMapping("/api/v1/realms/{realm}/clients/{clientId}/policies")
@Tag(name = "Policies", description = "Role-based Authorization Services (UMA2) policies")
public class PolicyController {

    private final KeycloakGateway keycloak;
    private final MessageInformationFactory messageInformationFactory;

    public PolicyController(KeycloakGateway keycloak, MessageInformationFactory messageInformationFactory) {
        this.keycloak = keycloak;
        this.messageInformationFactory = messageInformationFactory;
    }

    @PostMapping
    @Operation(summary = "Create a role-based policy",
            description = "Satisfied if the caller holds ANY ONE of roleNames.")
    public ResponseEntity<ApiResponse<PolicyResponse>> create(@PathVariable String realm, @PathVariable String clientId,
            @Valid @RequestBody CreateRolePolicyRequest request) {
        keycloak.createRolePolicy(realm, clientId, request.name(), request.roleNames());
        PolicyResponse created = new PolicyResponse(request.name(), request.roleNames());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Policy created", messageInformationFactory.create()));
    }

    @GetMapping
    @Operation(summary = "List policies under this client")
    public PagedApiResponse<PolicyResponse> list(
            @PathVariable String realm,
            @PathVariable String clientId,
            @Parameter(description = "1-based — page=1 is the first page.")
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be >= 1");
        }
        PagedPolicies result = keycloak.listPolicies(realm, clientId, page - 1, size);
        List<PolicyResponse> content = result.content().stream()
                .map(p -> new PolicyResponse(p.name(), p.roleNames()))
                .toList();
        return PagedApiResponse.success(
                content,
                PaginationInfo.of(page, size, result.totalElements()),
                messageInformationFactory.create());
    }

    @GetMapping("/{policyName}")
    @Operation(summary = "Get one policy")
    public ApiResponse<PolicyResponse> get(@PathVariable String realm, @PathVariable String clientId,
            @PathVariable String policyName) {
        PolicyResponse policy = keycloak.getPolicy(realm, clientId, policyName)
                .map(p -> new PolicyResponse(p.name(), p.roleNames()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No policy '" + policyName + "'"));
        return ApiResponse.success(policy, null, messageInformationFactory.create());
    }

    @DeleteMapping("/{policyName}")
    @Operation(summary = "Delete a policy")
    public ApiResponse<Void> delete(@PathVariable String realm, @PathVariable String clientId,
            @PathVariable String policyName) {
        keycloak.deletePolicy(realm, clientId, policyName);
        return ApiResponse.success(null, "Policy deleted", messageInformationFactory.create());
    }
}
