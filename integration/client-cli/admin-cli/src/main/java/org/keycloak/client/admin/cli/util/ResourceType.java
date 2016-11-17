package org.keycloak.client.admin.cli.util;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.client.admin.cli.commands.RealmOperations;
import org.keycloak.client.admin.cli.commands.UserOperations;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public enum ResourceType {

    REALM(RealmRepresentation.class),
    USER(UserRepresentation.class),
    ROLE(RoleRepresentation.class);

    private Class clazz;

    private <T> ResourceType(Class<T> c) {
        this.clazz = c;
    }

    public Class getType() {
        return clazz;
    }

    public String create(Keycloak client, String realm, Object representation) {
        if (RealmRepresentation.class == clazz) {
            return RealmOperations.create(client, (RealmRepresentation) representation);
        } else if (UserRepresentation.class == clazz) {
            return UserOperations.create(client, realm, (UserRepresentation) representation);
        } else {
            throw new RuntimeException("Unsupported type: " + clazz);
        }
    }

    public Object get(Keycloak client, String realm, String id) {
        if (RealmRepresentation.class == clazz) {
            return RealmOperations.get(client, id);
        } else if (UserRepresentation.class == clazz) {
            return UserOperations.get(client, realm, id);
        } else {
            throw new RuntimeException("Unsupported type: " + clazz);
        }
    }
}
