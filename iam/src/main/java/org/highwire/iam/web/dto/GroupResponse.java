package org.highwire.iam.web.dto;

import java.util.List;

public record GroupResponse(String name, List<String> roles) {
}
