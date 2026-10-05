package org.highwire.contentsync.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.highwire.authsdk.logging.AppLogger;
import org.highwire.authsdk.logging.Loggers;
import org.highwire.contentsync.generated.model.IngestRequest;
import org.springframework.stereotype.Service;

/**
 * Single Responsibility: owns ingest validation + the (currently logged,
 * not yet persisted — no atom store wired into this project) business
 * outcome. Nothing here knows about HTTP; {@link org.highwire.contentsync.web.IngestController}
 * owns that.
 *
 * IMPORTANT: validates by hand rather than trusting {@code @Valid} on the
 * generated {@link IngestRequest} — confirmed live that its Bean
 * Validation annotations are {@code javax.validation.constraints.*}
 * (swagger-codegen v3's default template), which Spring Boot 4's
 * Jakarta-based Hibernate Validator does not process. Relying on
 * {@code @Valid} here would silently validate nothing.
 */
@Service
public class IngestServiceImpl implements IngestService {

    private static final AppLogger log = Loggers.of(IngestServiceImpl.class);

    @Override
    public IngestResult ingest(IngestRequest request) {
        List<String> errors = validate(request);
        if (!errors.isEmpty()) {
            throw new IngestValidationException(errors);
        }

        String transactionId = request.getTransactionId() != null && !request.getTransactionId().isBlank()
                ? request.getTransactionId()
                : UUID.randomUUID().toString();

        log.info("Ingested path '{}' (peid={}, corpus={}, transactionId={})",
                request.getPath(), request.getPeid(), request.getCorpus(), transactionId);

        return new IngestResult(request.getPath(), request.getPeid(), request.getCorpus(), transactionId, Instant.now());
    }

    private List<String> validate(IngestRequest request) {
        List<String> errors = new ArrayList<>();
        if (isBlank(request.getPath())) {
            errors.add("path is required");
        } else if (!request.getPath().startsWith("/")) {
            errors.add("path must start with '/'");
        }
        if (isBlank(request.getPeid())) {
            errors.add("peid is required");
        }
        if (isBlank(request.getCorpus())) {
            errors.add("corpus is required");
        }
        if (isBlank(request.getXml())) {
            errors.add("xml is required");
        }
        return errors;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
