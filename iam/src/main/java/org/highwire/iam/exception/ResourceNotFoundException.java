package org.highwire.iam.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resourceType, String identifier) {
        super("No " + resourceType + " '" + identifier + "'");
    }
}
