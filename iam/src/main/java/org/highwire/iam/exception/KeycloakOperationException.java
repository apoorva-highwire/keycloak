package org.highwire.iam.exception;

/** Catch-all for an unexpected failure talking to Keycloak's Admin API — anything not one of the specific, expected exceptions above. */
public class KeycloakOperationException extends RuntimeException {
    public KeycloakOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
