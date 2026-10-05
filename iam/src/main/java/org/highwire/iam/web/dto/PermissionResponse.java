package org.highwire.iam.web.dto;

import java.util.List;

public record PermissionResponse(String name, String resourceName, List<String> scopes, List<String> policies) {
}
