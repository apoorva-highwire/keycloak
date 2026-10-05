package org.highwire.contentsync.web;

import org.highwire.contentsync.generated.model.IngestRequest;
import org.highwire.contentsync.service.IngestResult;
import org.highwire.contentsync.service.IngestService;
import org.highwire.contentsync.web.response.ApiResponse;
import org.highwire.contentsync.web.response.MessageInformationFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Single Responsibility: HTTP in, {@link ApiResponse} out — no validation
 * logic, no business logic. Depends on {@link IngestService} by
 * interface (Dependency Inversion), matching iam-service's own
 * controller/service separation and its ApiResponse envelope.
 *
 * {@code @Valid} deliberately NOT used on {@link IngestRequest} here —
 * see IngestServiceImpl's javadoc for why it would silently do nothing.
 */
@RestController
@RequestMapping("/ingest")
public class IngestController {

    private final IngestService ingestService;
    private final MessageInformationFactory messageInformationFactory;

    public IngestController(IngestService ingestService, MessageInformationFactory messageInformationFactory) {
        this.ingestService = ingestService;
        this.messageInformationFactory = messageInformationFactory;
    }

    @PostMapping
    public ApiResponse<IngestResult> ingest(@RequestBody IngestRequest request) {
        IngestResult result = ingestService.ingest(request);
        return ApiResponse.success(result, "Ingested", messageInformationFactory.create());
    }
}
