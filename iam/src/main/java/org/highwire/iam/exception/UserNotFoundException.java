package org.highwire.iam.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String realm, String username) {
        super("No user '" + username + "' in realm '" + realm + "'");
    }
}
