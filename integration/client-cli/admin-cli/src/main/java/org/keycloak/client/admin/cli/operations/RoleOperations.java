/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.client.admin.cli.operations;

import org.keycloak.client.admin.cli.util.HeadersBody;
import org.keycloak.client.admin.cli.util.HeadersBodyStatus;
import org.keycloak.client.admin.cli.util.HttpUtil;
import org.keycloak.client.admin.cli.util.Pair;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static org.keycloak.client.admin.cli.util.HttpUtil.composeResourceUrl;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class RoleOperations {

    public static class LIST_OF_ROLES extends ArrayList<RoleRepresentation>{};

    public static List<RoleRepresentation> getRealmRoles(String rootUrl, String realm, String auth) {
        String resourceUrl = composeResourceUrl(rootUrl, realm, "/roles");

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

        List<RoleRepresentation> roles;
        try {
            roles = JsonSerialization.readValue(response.getBody(), LIST_OF_ROLES.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read JSON response", e);
        }

        return roles;
    }

}
