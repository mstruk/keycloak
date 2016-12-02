package org.keycloak.client.admin.cli.commands;

import org.jboss.aesh.cl.Arguments;
import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import org.keycloak.client.admin.cli.config.ConfigData;
import java.util.List;

import static org.keycloak.client.admin.cli.operations.UserOperations.getIdFromUsername;
import static org.keycloak.client.admin.cli.operations.UserOperations.resetUserPassword;
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

    @Option(shortName = 'a', name = "admin-root", description = "URL of Admin REST endpoint root - e.g. http://localhost:8080/auth/admin", hasValue = true)
    String adminRestRoot;

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
        final String realm = getTargetRealm(config);
        final String adminRoot = adminRestRoot != null ? adminRestRoot : composeAdminRoot(server);

        // if username is specified resolve id
        if (username != null) {
            userid = getIdFromUsername(adminRoot, realm, auth, username);
        }

        resetUserPassword(server, realm, auth, userid, password, temporary);

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
