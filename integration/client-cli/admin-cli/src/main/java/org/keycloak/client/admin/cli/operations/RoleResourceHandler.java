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

import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class RoleResourceHandler extends ResourceHandler<RoleRepresentation> {

    protected String uid;
    protected String cid;
    protected String uname;
    protected String cname;

    public RoleResourceHandler(ResourceType type) {
        super(type);
    }

    @Override
    public String create(Keycloak client, String realm, RoleRepresentation representation) {
        return RoleOperations.create(client, realm, representation);
    }

    @Override
    public RoleRepresentation get(Keycloak client, String realm, String id) {
        return RoleOperations.get(client, realm, id);
    }

    @Override
    public void update(Keycloak client, String realm, RoleRepresentation representation) {
        RoleOperations.update(client, realm, representation);
    }

    @Override
    public void delete(Keycloak client, String realm, String id) {
        RoleOperations.delete(client, realm, id);
    }

    @Override
    public List<RoleRepresentation> getMany(Keycloak client, String realm, int offset, int limit, Map<String, String> filter) {
        if (uid != null && cid != null) {
            return RoleOperations.getClientRolesForUser(client, realm, uid, cid);
        }
        return RoleOperations.getAll(client, realm);
    }

    public void parseArgument(String option, Iterator<String> it) {
        switch (option) {
            case "--uname":
            case "--uid":
            case "--cname":
            case "--cid": {
                if (!it.hasNext()) {
                    throw new IllegalArgumentException("Option " + option + " requires a value");
                }
                break;
            }
            default: {
                throw new IllegalArgumentException("Invalid option: " + option);
            }
        }

        if ("--uname".equals(option)) {
            uname = it.next();
        } else if ("--uid".equals(option)) {
            uid = it.next();
        } else if ("--cname".equals(option)) {
            cname = it.next();
        } else if ("--cid".equals(option)) {
            cid = it.next();
        }
    }

    public void processArguments(Keycloak client, String realm) {
        // process --uname, --uid, --cname, --cid
        if (cid != null && cname != null) {
            throw new IllegalArgumentException("Parameters --cid and --cname are mutually exclusive");
        }
        if (uid != null && uname != null) {
            throw new IllegalArgumentException("Parameters --uid and --uname are mutually exclusive");
        }

        if (uname != null) {
            UserRepresentation rep = UserOperations.getByUsername(client, realm, uname);
            uid = rep.getId();
        }

        if (cname != null) {
            ClientRepresentation rep = ClientOperations.getByClientId(client, realm, cname);
            cid = rep.getId();
        }
    }
}
