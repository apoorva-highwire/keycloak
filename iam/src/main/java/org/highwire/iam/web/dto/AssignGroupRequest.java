package org.highwire.iam.web.dto;

import jakarta.validation.constraints.NotBlank;

public record AssignGroupRequest(@NotBlank String groupName) {
}
