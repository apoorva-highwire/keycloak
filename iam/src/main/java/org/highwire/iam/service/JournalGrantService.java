package org.highwire.iam.service;

import org.highwire.iam.gateway.KeycloakGateway;
import org.highwire.iam.web.dto.JournalGrantsResponse;
import org.springframework.stereotype.Service;

@Service
public class JournalGrantService {

    private final KeycloakGateway keycloak;

    public JournalGrantService(KeycloakGateway keycloak) {
        this.keycloak = keycloak;
    }

    public void grant(String realm, String username, String journalCode) {
        keycloak.grantJournal(realm, username, journalCode);
    }

    public void revoke(String realm, String username, String journalCode) {
        keycloak.revokeJournal(realm, username, journalCode);
    }

    public JournalGrantsResponse list(String realm, String username) {
        return new JournalGrantsResponse(realm, username, keycloak.getJournalCodes(realm, username));
    }
}
