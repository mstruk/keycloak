package org.keycloak.client.admin.cli.commands;

import org.jboss.aesh.cl.Arguments;
import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.client.admin.cli.config.ConfigData;
import org.keycloak.client.admin.cli.operations.UserOperations;
import org.keycloak.client.admin.cli.util.ConfigUtil;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.keycloak.client.admin.cli.util.AuthUtil.ensureToken;
import static org.keycloak.client.admin.cli.util.ConfigUtil.credentialsAvailable;
import static org.keycloak.client.admin.cli.util.ConfigUtil.loadConfig;
import static org.keycloak.client.admin.cli.util.OsUtil.CMD;
import static org.keycloak.client.admin.cli.util.OsUtil.EOL;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
@CommandDefinition(name = "set-password", description = "[ARGUMENTS]")
public class SetPasswordCmd extends AbstractAuthOptionsCmd implements Command {

    @Option(name = "username", description = "Username", hasValue = true)
    String username;

    @Option(name = "userid", description = "User ID", hasValue = true)
    String userid;

    @Option(shortName = 'p', name = "new-password", description = "New password", hasValue = true)
    String password;

    @Option(shortName = 't', name = "temporary", description = "is password temporary", hasValue = false, defaultValue = "false")
    boolean temporary;

    @Arguments
    List<String> args;


    @Override
    public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
        try {
            if (printHelp()) {
                return help ? CommandResult.SUCCESS : CommandResult.FAILURE;
            }

            processGlobalOptions();

            return process(commandInvocation);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(e.getMessage() + suggestHelp(), e);
        } finally {
            commandInvocation.stop();
        }
    }


    public CommandResult process(CommandInvocation commandInvocation) throws CommandException, InterruptedException {

        if (userid == null && username == null) {
            throw new IllegalArgumentException("No user specified. Use --username or --userid to specify user");
        }

        if (userid != null && username != null) {
            throw new IllegalArgumentException("Options --userid and --username are mutually exclusive");
        }

        if (password == null) {
            throw new IllegalArgumentException("No password specified");
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
        final String clientId = ConfigUtil.getEffectiveClientId(config);
        final String realm = getTargetRealm(config);


        // Initialize admin client Keycloak object
        // delegate to resource type create method
        Keycloak client = KeycloakBuilder.builder()
                .serverUrl(server)
                .realm(realm)
                .clientId(clientId)
                .authorization(auth)
                .build();

        UserRepresentation userRepresentation;

        // if username is specified resolve id
        if (username != null) {
            Map<String, String> filter = new HashMap<>();
            filter.put("username", username);

            List<UserRepresentation> users = UserOperations.getAllFiltered(client, realm, 0, 2, filter);
            if (users.size() > 1) {
                throw new RuntimeException("Multiple users found for username: " + username + ". Use --userid to specify user.");
            }

            if (users.size() == 0) {
                throw new RuntimeException("User not found for username: " + username);
            }
            userRepresentation = users.get(0);
        } else {
            userRepresentation = UserOperations.get(client, realm, userid);
            if (userRepresentation == null) {
                throw new RuntimeException("User not found for id: " + userid);
            }
        }

        UserOperations.resetPassword(client, realm, userRepresentation, password, temporary);

        return CommandResult.SUCCESS;
    }


    @Override
    protected boolean nothingToDo() {
        return noOptions() && username == null && userid == null && password == null && (args == null || args.size() == 0);
    }

    protected String suggestHelp() {
        return EOL + "Try '" + CMD + " help add-role' for more information";
    }

}
