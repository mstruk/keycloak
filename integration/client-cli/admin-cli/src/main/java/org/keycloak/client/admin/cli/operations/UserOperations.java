package org.keycloak.client.admin.cli.operations;

import org.keycloak.client.admin.cli.util.HeadersBody;
import org.keycloak.client.admin.cli.util.HeadersBodyStatus;
import org.keycloak.client.admin.cli.util.HttpUtil;
import org.keycloak.client.admin.cli.util.Pair;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.util.JsonSerialization;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static org.keycloak.client.admin.cli.util.HttpUtil.composeResourceUrl;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class UserOperations {

    public static void addRealmRoles(String rootUrl, String realm, String auth, String userid, List<RoleRepresentation> roles) {
        String resourceUrl = composeResourceUrl(rootUrl, realm, "users/" + userid + "/roles/role-mappings/realm");
        addRoles(resourceUrl, auth, roles);
    }

    public static void addClientRoles(String rootUrl, String realm, String auth, String userid, String idOfClient, List<RoleRepresentation> roles) {
        String resourceUrl = composeResourceUrl(rootUrl, realm, "users/" + userid + "/roles/role-mappings/clients/" + idOfClient);
        addRoles(resourceUrl, auth, roles);
    }

    public static void addRoles(String resourceUrl, String auth, List<RoleRepresentation> roles) {

        LinkedList<Pair> headers = new LinkedList<>();
        if (auth != null) {
            headers.add(new Pair("Authorization", auth));
        }
        headers.add(new Pair("Content-Type", "application/json"));

        HeadersBodyStatus response;

        byte[] body;
        try {
            body = JsonSerialization.writeValueAsBytes(roles);
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize JSON", e);
        }

        try {
            response = HttpUtil.doRequest("post", resourceUrl, new HeadersBody(headers, new ByteArrayInputStream(body)));
        } catch (IOException e) {
            throw new RuntimeException("HTTP request failed: POST " + resourceUrl + "\n" + new String(body), e);
        }

        response.checkSuccess();
    }

    public static void resetUserPassword(String rootUrl, String realm, String auth, String userid, String password, boolean temporary) {

        String resourceUrl = composeResourceUrl(rootUrl, realm, "users/" + userid + "/reset-password");

        LinkedList<Pair> headers = new LinkedList<>();
        if (auth != null) {
            headers.add(new Pair("Authorization", auth));
        }
        headers.add(new Pair("Content-Type", "application/json"));

        CredentialRepresentation credentials = new CredentialRepresentation();
        credentials.setType("password");
        credentials.setTemporary(temporary);
        credentials.setValue(password);

        HeadersBodyStatus response;

        byte[] body;
        try {
            body = JsonSerialization.writeValueAsBytes(credentials);
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize JSON", e);
        }

        try {
            response = HttpUtil.doRequest("put", resourceUrl, new HeadersBody(headers, new ByteArrayInputStream(body)));
        } catch (IOException e) {
            throw new RuntimeException("HTTP request failed: PUT " + resourceUrl + "\n" + new String(body), e);
        }

        response.checkSuccess();
    }

    public static String getIdFromUsername(String rootUrl, String realm, String auth, String username) {

        String resourceUrl = composeResourceUrl(rootUrl, realm, "users");

        resourceUrl = HttpUtil.addQueryParamsToUri(resourceUrl, "username", username, "first", "0", "max", "2");

        LinkedList<Pair> headers = new LinkedList<>();
        if (auth != null) {
            headers.add(new Pair("Authorization", auth));
        }
        headers.add(new Pair("Accept", "application/json"));

        HeadersBodyStatus response;
        try {
            response = HttpUtil.doRequest("get", resourceUrl, new HeadersBody(headers));
        } catch (IOException e) {
            throw new RuntimeException("HTTP request failed: GET " + resourceUrl, e);
        }

        response.checkSuccess();

        List<UserRepresentation> users;
        try {
            users = JsonSerialization.readValue(response.getBody(), new ArrayList<UserRepresentation>(){}.getClass());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read JSON response", e);
        }

        if (users.size() > 1) {
            throw new RuntimeException("Multiple users found for username: " + username + ". Use --userid to specify user.");
        }

        if (users.size() == 0) {
            throw new RuntimeException("User not found for username: " + username);
        }

        return users.get(0).getId();
    }
}
