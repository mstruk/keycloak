package org.keycloak.client.admin.cli.commands;

import org.jboss.aesh.cl.Arguments;
import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.invocation.CommandInvocation;

import java.util.List;

import static org.keycloak.client.admin.cli.util.IoUtil.printOut;


/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
@CommandDefinition(name = "help", description = "This help")
public class HelpCmd implements Command {

    @Arguments
    private List<String> args;

    @Override
    public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
        try {
            if (args == null || args.size() == 0) {
                printOut(KcAdmCmd.usage());
            } else {
                outer:
                switch (args.get(0)) {
                    case "config": {
                        if (args.size() > 1) {
                            switch (args.get(1)) {
                                case "credentials": {
                                    printOut(ConfigCredentialsCmd.usage());
                                    break outer;
                                }
                                case "truststore": {
                                    printOut(ConfigTruststoreCmd.usage());
                                    break outer;
                                }
                            }
                        }
                        printOut(ConfigCmd.usage());
                        break;
                    }
                    default: {
                        throw new RuntimeException("Unknown command: " + args.get(0));
                    }
                }
            }

            return CommandResult.SUCCESS;
        } finally {
            commandInvocation.stop();
        }
    }
}
