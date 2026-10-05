package org.highwire.iam.web.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;

public record RealmJournalsRequest(@NotNull List<String> journals) {
}
