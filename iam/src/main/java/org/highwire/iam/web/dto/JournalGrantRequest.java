package org.highwire.iam.web.dto;

import jakarta.validation.constraints.NotBlank;

public record JournalGrantRequest(@NotBlank String journalCode) {
}
