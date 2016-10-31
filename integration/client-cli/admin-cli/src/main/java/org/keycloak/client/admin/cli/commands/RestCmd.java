package org.keycloak.client.admin.cli.commands;

import org.jboss.aesh.cl.Arguments;
import org.jboss.aesh.cl.GroupCommandDefinition;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.invocation.CommandInvocation;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import static org.keycloak.client.admin.cli.util.OsUtil.CMD;
import static org.keycloak.client.admin.cli.util.OsUtil.EOL;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
@GroupCommandDefinition(name = "rest", description = "COMMAND [ARGUMENTS]", groupCommands = {ConfigCredentialsCmd.class, RestPostCmd.class} )
public class RestCmd extends AbstractAuthOptionsCmd implements Command {

    @Arguments
    protected List<String> args;


    public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
        try {
            if (args != null && args.size() > 0) {
                String cmd = args.get(0);
                switch (cmd) {
                    case "post": {
                        RestPostCmd command = RestPostCmd.newInstance(this);
                        return command.execute(commandInvocation);
                    }
                    default: {
                        if (printHelp()) {
                            return help ? CommandResult.SUCCESS : CommandResult.FAILURE;
                        }
                        throw new IllegalArgumentException("Unknown sub-command: " + cmd + suggestHelp());
                    }
                }
            }

            if (printHelp()) {
                return help ? CommandResult.SUCCESS : CommandResult.FAILURE;
            }

            throw new IllegalArgumentException("Sub-command required by '" + CMD + " config' - one of: 'get', 'post', 'put', 'delete'");

        } finally {
            commandInvocation.stop();
        }
    }

    protected String suggestHelp() {
        return EOL + "Try '" + CMD + " help rest' for more information";
    }

    protected String help() {
        return usage();
    }

    public static String usage() {
        StringWriter sb = new StringWriter();
        PrintWriter out = new PrintWriter(sb);
        out.println("Usage: " + CMD + " rest SUB_COMMAND [ARGUMENTS]");
        out.println();
        out.println("Where SUB_COMMAND is one of: 'get', 'post', 'put', 'delete'");
        out.println();
        out.println();
        out.println("Use '" + CMD + " help rest SUB_COMMAND' for more info.");
        out.println("Use '" + CMD + " help' for general information and a list of commands.");
        return sb.toString();
    }
}
