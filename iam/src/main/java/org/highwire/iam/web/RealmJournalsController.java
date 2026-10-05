package org.highwire.iam.web;

import java.util.List;

import org.highwire.authsdk.caller.CallerContext;
import org.highwire.authsdk.caller.CallerInfo;
import org.highwire.authsdk.caller.CallerScope;
import org.highwire.iam.gateway.KeycloakGateway;
import org.highwire.iam.web.dto.RealmJournalsRequest;
import org.highwire.iam.web.response.ApiResponse;
import org.highwire.iam.web.response.MessageInformationFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * The authoritative "which journals does this publisher have" list — a
 * realm attribute, not derived from which Authorization Services
 * resources happen to exist yet. Lets a UI show a journal as declared-
 * but-not-yet-configured, rather than invisible, between creating the
 * realm role and running the resource/policy/permission setup.
 */
@RestController
@RequestMapping("/api/v1/realms/{realm}/journals")
@Tag(name = "Realm Journals", description = "The publisher's own declared list of journal codes")
public class RealmJournalsController {

    private final KeycloakGateway keycloak;
    private final MessageInformationFactory messageInformationFactory;

    public RealmJournalsController(KeycloakGateway keycloak, MessageInformationFactory messageInformationFactory) {
        this.keycloak = keycloak;
        this.messageInformationFactory = messageInformationFactory;
    }

    @GetMapping
    @Operation(summary = "List declared journal codes",
            description = "A JOURNAL-scoped caller sees only its own journal (if declared at all); "
                    + "PUBLISHER/SERVICE sees the full list — enforced here, not left to a UI to filter.")
    public ApiResponse<List<String>> list(@PathVariable String realm) {
        List<String> all = keycloak.getRealmJournals(realm);
        CallerInfo caller = CallerContext.get();
        List<String> visible = caller.scope() == CallerScope.JOURNAL
                ? all.stream().filter(code -> code.equals(caller.journal())).toList()
                : all;
        return ApiResponse.success(visible, null, messageInformationFactory.create());
    }

    @PutMapping
    @Operation(summary = "Replace the publisher's declared journal list",
            description = "Publisher-level action — a journal-scoped caller can read this list but not change it.")
    public ApiResponse<List<String>> replace(@PathVariable String realm, @Valid @RequestBody RealmJournalsRequest request) {
        requireAdminCaller();
        keycloak.setRealmJournals(realm, request.journals());
        return ApiResponse.success(request.journals(), "Journals updated", messageInformationFactory.create());
    }

    /** Same rule as UserController's own group/role assignment endpoints — a journal-scoped caller never administers, only reads. */
    private void requireAdminCaller() {
        if (CallerContext.get().scope() == CallerScope.JOURNAL) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not permitted for a journal-scoped caller");
        }
    }
}
