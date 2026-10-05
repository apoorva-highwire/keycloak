package org.highwire.contentsync.service;

import org.highwire.contentsync.generated.model.IngestRequest;

/**
 * Narrow on purpose (Interface Segregation) — one capability, so
 * {@link org.highwire.contentsync.web.IngestController} depends on this
 * contract (Dependency Inversion), not on {@link IngestServiceImpl}
 * directly. A future real implementation (e.g. a MarkLogic-backed one,
 * matching impl/content-sync-poc's AtomStoreService) can replace the impl
 * without touching the controller — Open/Closed.
 */
public interface IngestService {

    IngestResult ingest(IngestRequest request);
}
