package org.highwire.iam.web;

import java.util.List;

import org.highwire.iam.service.JournalGrantService;
import org.highwire.iam.web.dto.JournalGrantRequest;
import org.highwire.iam.web.response.ApiResponse;
import org.highwire.iam.web.response.MessageInformationFactory;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/realms/{realm}/users/{username}/journal-grants")
@Tag(name = "Journal grants", description = "Per-reader journal entitlements, stored as a Keycloak user attribute")
public class JournalGrantController {

    private final JournalGrantService grants;
    private final MessageInformationFactory messageInformationFactory;

    public JournalGrantController(JournalGrantService grants, MessageInformationFactory messageInformationFactory) {
        this.grants = grants;
        this.messageInformationFactory = messageInformationFactory;
    }

    @GetMapping
    @Operation(summary = "List a reader's current journal entitlements")
    public ApiResponse<List<String>> list(@PathVariable String realm, @PathVariable String username) {
        List<String> codes = grants.list(realm, username).journalCodes();
        return ApiResponse.success(codes, null, messageInformationFactory.create());
    }

    @PostMapping
    @Operation(summary = "Grant a reader access to one journal",
            description = "Idempotent — granting a journal the reader already has is a no-op. "
                    + "Takes effect on the reader's NEXT token issuance, not retroactively on tokens "
                    + "already held.")
    public ApiResponse<Void> grant(@PathVariable String realm, @PathVariable String username,
            @Valid @RequestBody JournalGrantRequest request) {
        grants.grant(realm, username, request.journalCode());
        return ApiResponse.success(null, "Journal granted", messageInformationFactory.create());
    }

    @DeleteMapping("/{journalCode}")
    @Operation(summary = "Revoke a reader's access to one journal",
            description = "Idempotent — revoking a journal the reader doesn't have is a no-op. "
                    + "Does not invalidate a token already issued; see the service-level note on revocation.")
    public ApiResponse<Void> revoke(@PathVariable String realm, @PathVariable String username,
            @PathVariable String journalCode) {
        grants.revoke(realm, username, journalCode);
        return ApiResponse.success(null, "Journal revoked", messageInformationFactory.create());
    }
}
