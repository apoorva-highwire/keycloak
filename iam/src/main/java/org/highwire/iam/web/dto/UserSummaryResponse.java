package org.highwire.iam.web.dto;

import java.util.List;

public record UserSummaryResponse(String username, List<String> journalCodes) {
}
