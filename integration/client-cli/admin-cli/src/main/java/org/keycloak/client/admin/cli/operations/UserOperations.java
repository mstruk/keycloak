package org.keycloak.client.admin.cli.operations;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.client.admin.cli.util.HttpUtil;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class UserOperations {

    public static String create(Keycloak client, String realm, UserRepresentation user) {
        Response response = client.realm(realm).users().create(user);
        HttpUtil.checkStatusCreated(response);
        return HttpUtil.extractIdFromLocation(response.getLocation().toString());
    }

    public static UserRepresentation get(Keycloak client, String realm, String id) {
        return client.realm(realm).users().get(id).toRepresentation();
    }

    public static List<UserRepresentation> getAll(Keycloak client, String realm, int offset, int limit) {
        return client.realm(realm).users().search(null, offset, limit);
    }

    public static UserRepresentation getByUsername(Keycloak client, String realm, String username) {
        Map<String, String> filter = new HashMap<>();
        filter.put("username", username);
        List<UserRepresentation> result = getAllFiltered(client, realm, 0, 2, filter);
        if (result.size() > 1) {
            throw new RuntimeException("More that one user found for username: " + username);
        }
        return result.size() > 0 ? result.get(0) : null;
    }
    public static List<UserRepresentation> getAllFiltered(Keycloak client, String realm, int offset, int limit, Map<String, String> filter) {
        String username = null;
        String firstName = null;
        String lastName = null;
        String email = null;
        String search = null;

        for (Map.Entry<String, String> item: filter.entrySet()) {
            switch(item.getKey()) {
                case "username":
                    username = item.getValue();
                    break;
                case "firstName":
                    firstName = item.getValue();
                    break;
                case "lastName":
                    lastName = item.getValue();
                    break;
                case "email":
                    email = item.getValue();
                    break;
                case "search":
                case "":
                    search = item.getValue();
                    break;
                default:
                    throw new RuntimeException("Unsupported search parameter: " + item.getKey() + " (should be one of: username, firstName, lastName, email, search)");
            }
        }

        if (search != null && (username != null || firstName != null || lastName != null || email != null)) {
            throw new RuntimeException("Incompatible search parameters - when 'search' is specified no other search parameter can be specified");
        }

        if (username != null || firstName != null || lastName != null || email != null) {
            return client.realm(realm).users().search(username, firstName, lastName, email, offset, limit);
        } else {
            return client.realm(realm).users().search(search, offset, limit);
        }
    }

    public static void update(Keycloak client, String realm, UserRepresentation user) {
        if (user.getId() == null) {
            throw new RuntimeException("User has no id set");
        }
        client.realm(realm).users().get(user.getId()).update(user);
    }

    public static void delete(Keycloak client, String realm, String id) {
        Response response = client.realm(realm).users().delete(id);
        HttpUtil.checkStatusNoContent(response);
    }

    public static void addRoles(Keycloak client, String realm, String id, List<RoleRepresentation> roles) {
        client.realm(realm).users().get(id).roles().realmLevel().add(roles);
    }

    public static void addClientRoles(Keycloak client, String realm, String id, String idOfClient, List<RoleRepresentation> roles) {
        client.realm(realm).users().get(id).roles().clientLevel(idOfClient).add(roles);
    }
}
