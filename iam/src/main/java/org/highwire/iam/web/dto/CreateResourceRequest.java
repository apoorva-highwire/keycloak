package org.highwire.iam.web.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record CreateResourceRequest(@NotBlank String name, @NotEmpty List<String> scopes) {
}
