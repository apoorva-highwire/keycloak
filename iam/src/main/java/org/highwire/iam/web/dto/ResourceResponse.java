package org.highwire.iam.web.dto;

import java.util.List;

public record ResourceResponse(String name, List<String> scopes) {
}
