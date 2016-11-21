package org.keycloak.client.admin.cli.commands;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.client.admin.cli.util.HttpUtil;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import javax.ws.rs.core.Response;
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
}
