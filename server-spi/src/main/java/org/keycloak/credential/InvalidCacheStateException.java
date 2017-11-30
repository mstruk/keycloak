package org.keycloak.credential;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class InvalidCacheStateException extends IllegalStateException {

    public InvalidCacheStateException(String message) {
        super(message);
    }
}
