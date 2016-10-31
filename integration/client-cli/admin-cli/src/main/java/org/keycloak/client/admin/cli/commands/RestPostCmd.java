package org.keycloak.client.admin.cli.commands;

import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import org.keycloak.client.admin.cli.util.HeadersBody;
import org.keycloak.client.admin.cli.util.HeadersBodyStatus;
import org.keycloak.client.admin.cli.util.HttpUtil;
import org.keycloak.client.admin.cli.util.IoUtil;
import org.keycloak.client.admin.cli.util.Pair;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.keycloak.client.admin.cli.util.IoUtil.copyStream;
import static org.keycloak.client.admin.cli.util.IoUtil.printOut;
import static org.keycloak.client.admin.cli.util.IoUtil.readFileOrStdin;
import static org.keycloak.client.admin.cli.util.OsUtil.CMD;
import static org.keycloak.client.admin.cli.util.OsUtil.EOL;
import static org.keycloak.client.admin.cli.util.ParseUtil.parseKeyVal;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class RestPostCmd extends AbstractAuthOptionsCmd implements Command {

    private RestCmd parent;

    public static RestPostCmd newInstance(RestCmd parent) {
        RestPostCmd cmd = new RestPostCmd();
        cmd.parent = parent;
        cmd.initFromParent(parent);
        return cmd;
    }

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

        List<Pair> headers = new ArrayList<>();

        List<String> args = parent.args.subList(1, parent.args.size());

        if (args == null || args.isEmpty()) {
            throw new IllegalArgumentException("URL not specified");
        }

        if (args.size() > 1) {
            throw new IllegalArgumentException("Invalid option: " + args.get(1));
        }

        String url = null;
        String file = null;

        if (args != null) {
            Iterator<String> it = args.iterator();
            while (it.hasNext()) {
                String option = it.next();
                switch (option) {
                    case "-h":
                    case "--header": {
                        requireValue(it, option);
                        String[] keyVal = parseKeyVal(it.next());
                        headers.add(new Pair(keyVal[0], keyVal[1]));
                        break;
                    }
                    case "-f":
                    case "--file": {
                        requireValue(it, option);
                        file = it.next();
                    }
                    default: {
                        if (url == null) {
                            url = option;
                        } else {
                            throw new IllegalArgumentException("Unsupported option: " + option);
                        }
                    }
                }
            }
        }

        InputStream body = null;
        if (file != null) {
            if ("-".equals(file)) {
                body = System.in;
            } else {
                try {
                    body = new BufferedInputStream(new FileInputStream(file));
                } catch (FileNotFoundException e) {
                    throw new RuntimeException("File not found: " + file);
                }
            }
        }

        HeadersBodyStatus response;
        try {
            response = HttpUtil.doRequest(getType(), url, new HeadersBody(headers, body));
        } catch (IOException e) {
            throw new RuntimeException("HTTP request error: " + e.getMessage(), e);
        }

        // output response
        printOut(response.getStatus());
        for (Pair pair: response.getHeaders()) {
            printOut(pair.getKey() + ": " + pair.getValue());
        }
        printOut("");

        if (response.getBody() != null) {
            copyStream(response.getBody(), System.out);
        }

        return CommandResult.SUCCESS;
    }

    protected String getType() {
        return "post";
    }

    private void requireValue(Iterator<String> it, String option) {
        if (!it.hasNext()) {
            throw new IllegalArgumentException("Option " + option + " requires a value");
        }
    }

    protected String suggestHelp() {
        return EOL + "Try '" + CMD + " help rest post' for more information";
    }
}
