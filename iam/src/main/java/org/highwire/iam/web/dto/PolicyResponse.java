package org.highwire.iam.web.dto;

import java.util.List;

public record PolicyResponse(String name, List<String> roleNames) {
}
