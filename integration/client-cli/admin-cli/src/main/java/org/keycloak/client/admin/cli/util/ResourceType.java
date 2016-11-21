package org.keycloak.client.admin.cli.util;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.client.admin.cli.commands.RealmOperations;
import org.keycloak.client.admin.cli.commands.RoleOperations;
import org.keycloak.client.admin.cli.commands.UserOperations;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public enum ResourceType {

    REALMS(RealmRepresentation.class, true),
    REALM(RealmRepresentation.class, false),
    USERS(UserRepresentation.class, true),
    USER(UserRepresentation.class, false),
    ROLES(RoleRepresentation.class, true),
    ROLE(RoleRepresentation.class, false);

    private Class clazz;
    private boolean collection;

    private <T> ResourceType(Class<T> c, boolean collection) {
        this.clazz = c;
        this.collection = collection;
    }

    public Class getType() {
        return clazz;
    }

    public String create(Keycloak client, String realm, Object representation) {
        if (RealmRepresentation.class == clazz) {
            return RealmOperations.create(client, (RealmRepresentation) representation);
        } else if (UserRepresentation.class == clazz) {
            return UserOperations.create(client, realm, (UserRepresentation) representation);
        } else if (RoleRepresentation.class == clazz) {
            return RoleOperations.create(client, realm, (RoleRepresentation) representation);
        } else {
            throw new RuntimeException("Unsupported type: " + clazz);
        }
    }

    public Object get(Keycloak client, String realm, String id) {
        if (RealmRepresentation.class == clazz) {
            return RealmOperations.get(client, id);
        } else if (UserRepresentation.class == clazz) {
            return UserOperations.get(client, realm, id);
        } else if (RoleRepresentation.class == clazz) {
            return RoleOperations.get(client, realm, id);
        } else {
            throw new RuntimeException("Unsupported type: " + clazz);
        }
    }

    public List<?> getMany(Keycloak client, String realm, int offset, int limit, Map<String, String> filter) {
        if (RealmRepresentation.class == clazz) {
            return RealmOperations.getAll(client);
        } else if (UserRepresentation.class == clazz) {
            return UserOperations.getAllFiltered(client, realm, offset, limit, filter);
        } else if (RoleRepresentation.class == clazz) {
            return RoleOperations.getAll(client, realm);
        } else {
            throw new RuntimeException("Unsupported type: " + clazz);
        }
    }

    public void update(Keycloak client, String realm, Object representation) {
        if (RealmRepresentation.class == clazz) {
            RealmOperations.update(client, (RealmRepresentation) representation);
        } else if (UserRepresentation.class == clazz) {
            UserOperations.update(client, realm, (UserRepresentation) representation);
        } else if (RoleRepresentation.class == clazz) {
            RoleOperations.update(client, realm, (RoleRepresentation) representation);
        } else {
            throw new RuntimeException("Unsupported type: " + clazz);
        }
    }

    public void delete(Keycloak client, String realm, String id) {
        if (RealmRepresentation.class == clazz) {
            RealmOperations.delete(client, id);
        } else if (UserRepresentation.class == clazz) {
            UserOperations.delete(client, realm, id);
        } else if (RoleRepresentation.class == clazz) {
            RoleOperations.delete(client, realm, id);
        } else {
            throw new RuntimeException("Unsupported type: " + clazz);
        }
    }

    public boolean isCollectionType() {
        return collection;
    }
}
