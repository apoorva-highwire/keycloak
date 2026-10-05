package org.highwire.iam.web.dto;

import java.util.List;

public record JournalGrantsResponse(String realm, String username, List<String> journalCodes) {
}
