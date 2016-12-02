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
package org.keycloak.client.admin.cli.commands;

import org.jboss.aesh.cl.Arguments;
import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import org.keycloak.client.admin.cli.common.AttributeOperation;
import org.keycloak.client.admin.cli.config.ConfigData;
import org.keycloak.client.admin.cli.operations.ClientOperations;
import org.keycloak.client.admin.cli.operations.RoleOperations;
import org.keycloak.client.admin.cli.operations.RoleSearch;
import org.keycloak.client.admin.cli.operations.UserOperations;
import org.keycloak.representations.idm.RoleRepresentation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static org.keycloak.client.admin.cli.operations.UserOperations.getIdFromUsername;
import static org.keycloak.client.admin.cli.util.AuthUtil.ensureToken;
import static org.keycloak.client.admin.cli.util.ConfigUtil.credentialsAvailable;
import static org.keycloak.client.admin.cli.util.ConfigUtil.loadConfig;
import static org.keycloak.client.admin.cli.util.OsUtil.CMD;
import static org.keycloak.client.admin.cli.util.OsUtil.EOL;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
@CommandDefinition(name = "add-role", description = "[ARGUMENTS]")
public class AddRoleCmd extends AbstractAuthOptionsCmd implements Command {

    @Option(shortName = 'a', name = "admin-root", description = "URL of Admin REST endpoint root - e.g. http://localhost:8080/auth/admin", hasValue = true)
    String adminRestRoot;

    @Option(name = "username", description = "Read object from file or standard input if FILENAME is set to '-'", hasValue = true)
    String username;

    @Option(name = "userid", description = "Read object from file or standard input if FILENAME is set to '-'", hasValue = true)
    String userid;

    //@OptionGroup(name = "rolename", description = "Read object from file or standard input if FILENAME is set to '-'", hasValue = true)
    //pString rolename;

    //@OptionGroup(name = "roleid", description = "Read object from file or standard input if FILENAME is set to '-'", hasValue = true)
    //String roleid;

    @Option(name = "clientid", description = "Read object from file or standard input if FILENAME is set to '-'", hasValue = true)
    String clientid;

    @Arguments
    List<String> args;


    @Override
    public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {

        List<String> roleNames = new LinkedList<>();
        List<String> roleIds = new LinkedList<>();

        try {
            if (printHelp()) {
                return help ? CommandResult.SUCCESS : CommandResult.FAILURE;
            }

            processGlobalOptions();

            Iterator<String> it = args.iterator();

            while (it.hasNext()) {
                String option = it.next();
                switch (option) {
                    case "--rolename": {
                        optionRequiresValueCheck(it, option);
                        roleNames.add(it.next());
                        break;
                    }
                    case "--roleid": {
                        optionRequiresValueCheck(it, option);
                        roleIds.add(it.next());
                        break;
                    }
                    default: {
                        throw new IllegalArgumentException("Invalid option: " + option);
                    }
                }
            }

            if (userid == null && username == null) {
                throw new IllegalArgumentException("No user specified. Use --username or --userid to specify user");
            }

            if (userid != null && username != null) {
                throw new IllegalArgumentException("Options --userid and --username are mutually exclusive");
            }

            if (roleNames.isEmpty() && roleIds.isEmpty()) {
                throw new IllegalArgumentException("No role specified. Use --rolename or --roleid to specify roles");
            }

            ConfigData config = loadConfig();
            config = copyWithServerInfo(config);

            setupTruststore(config, commandInvocation);

            String auth = null;

            config = ensureAuthInfo(config, commandInvocation);
            config = copyWithServerInfo(config);
            if (credentialsAvailable(config)) {
                auth = ensureToken(config);
            }

            auth = auth != null ? "Bearer " + auth : null;

            final String server = config.getServerUrl();
            final String realm = getTargetRealm(config);
            final String adminRoot = adminRestRoot != null ? adminRestRoot : composeAdminRoot(server);

            // if username is specified resolve id
            if (username != null) {
                userid = getIdFromUsername(adminRoot, realm, auth, username);
            }


            if (clientid != null) {
                String idOfClient = ClientOperations.getIdFromClientId(adminRoot, realm, auth, clientid);

                List<RoleRepresentation> roles = ClientOperations.getRolesForClient(adminRoot, realm, auth, idOfClient);
                Set<RoleRepresentation> rolesToAdd = getRoleRepresentations(roleNames, roleIds, new RoleSearch(roles));

                // now add all the roles
                UserOperations.addClientRoles(adminRoot, realm, auth, userid, idOfClient, new ArrayList<>(rolesToAdd));
            } else {
                Set<RoleRepresentation> rolesToAdd = getRoleRepresentations(roleNames, roleIds,
                        new RoleSearch(RoleOperations.getRealmRoles(adminRoot, realm, auth)));

                // now add all the roles
                UserOperations.addRealmRoles(adminRoot, realm, auth, userid, new ArrayList<>(rolesToAdd));
            }
            return CommandResult.SUCCESS;

        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(e.getMessage() + suggestHelp(), e);
        } finally {
            commandInvocation.stop();
        }
    }

    private Set<RoleRepresentation> getRoleRepresentations(List<String> roleNames, List<String> roleIds, RoleSearch roleSearch) {
        Set<RoleRepresentation> rolesToAdd = new HashSet<>();

        // now we process roles
        for (String name : roleNames) {
            RoleRepresentation r = roleSearch.findByName(name);
            if (r == null) {
                throw new RuntimeException("Role not found for rolename: " + name);
            }
            rolesToAdd.add(r);
        }
        for (String id : roleIds) {
            RoleRepresentation r = roleSearch.findById(id);
            if (r == null) {
                throw new RuntimeException("Role not found for roleid: " + id);
            }
            rolesToAdd.add(r);
        }
        return rolesToAdd;
    }

    private void optionRequiresValueCheck(Iterator<String> it, String option) {
        if (!it.hasNext()) {
            throw new IllegalArgumentException("Option " + option + " requires a value");
        }
    }

    @Override
    protected boolean nothingToDo() {
        return noOptions() && username == null && userid == null && clientid == null && (args == null || args.size() == 0);
    }

    protected String suggestHelp() {
        return EOL + "Try '" + CMD + " help add-role' for more information";
    }
}
