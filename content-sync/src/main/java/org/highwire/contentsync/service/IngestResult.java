package org.highwire.contentsync.service;

import java.time.Instant;

/** What ingesting one path actually produced — the service layer's own return type, not the generated DTO. */
public record IngestResult(String path, String peid, String corpus, String transactionId, Instant ingestedAt) {
}
