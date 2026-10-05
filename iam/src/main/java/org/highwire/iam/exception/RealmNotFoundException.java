package org.highwire.iam.exception;

public class RealmNotFoundException extends RuntimeException {
    public RealmNotFoundException(String realm) {
        super("No realm '" + realm + "'");
    }
}
