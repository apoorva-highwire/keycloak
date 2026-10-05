package org.highwire.iam.exception;

public class ResourceAlreadyExistsException extends RuntimeException {
    public ResourceAlreadyExistsException(String resourceType, String identifier) {
        super(resourceType + " '" + identifier + "' already exists");
    }
}
