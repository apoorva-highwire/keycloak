package org.highwire.iam.web.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

/** roleNames: satisfied if the caller holds ANY ONE of these realm roles. */
public record CreateRolePolicyRequest(@NotBlank String name, @NotEmpty List<String> roleNames) {
}
