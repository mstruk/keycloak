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
import org.keycloak.client.admin.cli.common.AttributeOperation;
import org.keycloak.client.admin.cli.common.CmdStdinContext;
import org.keycloak.client.admin.cli.config.ConfigData;
import org.keycloak.client.admin.cli.operations.ResourceHandler;
import org.keycloak.client.admin.cli.operations.ResourceType;
import org.keycloak.client.admin.cli.util.ConfigUtil;
import org.keycloak.util.JsonSerialization;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import static org.keycloak.client.admin.cli.common.AttributeOperation.Type.SET;
import static org.keycloak.client.admin.cli.util.AuthUtil.ensureToken;
import static org.keycloak.client.admin.cli.util.ConfigUtil.DEFAULT_CONFIG_FILE_STRING;
import static org.keycloak.client.admin.cli.util.ConfigUtil.credentialsAvailable;
import static org.keycloak.client.admin.cli.util.ConfigUtil.loadConfig;
import static org.keycloak.client.admin.cli.util.IoUtil.printErr;
import static org.keycloak.client.admin.cli.util.IoUtil.printOut;
import static org.keycloak.client.admin.cli.util.OsUtil.CMD;
import static org.keycloak.client.admin.cli.util.OsUtil.EOL;
import static org.keycloak.client.admin.cli.util.OsUtil.OS_ARCH;
import static org.keycloak.client.admin.cli.util.OsUtil.PROMPT;
import static org.keycloak.client.admin.cli.util.ParseUtil.checkResourceType;
import static org.keycloak.client.admin.cli.util.ParseUtil.mergeAttributes;
import static org.keycloak.client.admin.cli.util.ParseUtil.parseFileOrStdin;
import static org.keycloak.client.admin.cli.util.ParseUtil.parseKeyVal;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
@CommandDefinition(name = "create", description = "Command to create new resources")
public class CreateCmd extends AbstractAuthOptionsCmd implements Command {

    @Option(shortName = 'i', name = "id", description = "After creation only print id of created resource to standard output", hasValue = false)
    protected boolean returnId = false;

    @Option(shortName = 'f', name = "file", description = "Read object from file or standard input if FILENAME is set to '-'", hasValue = true)
    protected String file;

    @Option(shortName = 'o', name = "output", description = "After creation output the new resource to standard output", hasValue = false)
    protected boolean outputResult = false;

    @Option(shortName = 'c', name = "compressed", description = "Don't pretty print the output", hasValue = false)
    protected boolean compressed = false;

    //@OptionGroup(shortName = 's', name = "set", description = "Set attribute to the specified value")
    //Map<String, String> attributes = new LinkedHashMap<>();

    @Arguments
    protected List<String> args;

    @Override
    public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {

        List<AttributeOperation> attrs = new LinkedList<>();

        ResourceType resourceType = null;

        try {
            if (printHelp()) {
                return help ? CommandResult.SUCCESS : CommandResult.FAILURE;
            }

            processGlobalOptions();

            if (args == null || args.isEmpty()) {
                throw new IllegalArgumentException("TYPE not specified");
            }

            Iterator<String> it = args.iterator();

            resourceType = checkResourceType(it.next());
            ResourceHandler handler = resourceType.newHandler();

            while (it.hasNext()) {
                String option = it.next();
                switch (option) {
                    case "-s":
                    case "--set": {
                        if (!it.hasNext()) {
                            throw new IllegalArgumentException("Option " + option + " requires a value");
                        }
                        String[] keyVal = parseKeyVal(it.next());
                        attrs.add(new AttributeOperation(SET, keyVal[0], keyVal[1]));
                        break;
                    }
                    default: {
                        throw new IllegalArgumentException("Invalid option: " + option);
                    }
                }
            }

            if (file == null && attrs.size() == 0) {
                throw new IllegalArgumentException("No file nor attribute values specified");
            }

            if (outputResult && returnId) {
                throw new IllegalArgumentException("Options -o and -i are mutually exclusive");
            }


            CmdStdinContext ctx = new CmdStdinContext();
            if (file != null) {
                ctx = parseFileOrStdin(file, resourceType.getType());
            }

            if (attrs.size() > 0) {
                try {
                    ctx = mergeAttributes(ctx, resourceType.getType().getConstructor(null), attrs);
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException("Failed to instantiate representation object", e);
                }
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

            String id = handler.create(client, realm, ctx.getResult());

            outputResult(handler, client, realm, id);


            return CommandResult.SUCCESS;

        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(e.getMessage() + suggestHelp(), e);
        } finally {
            commandInvocation.stop();
        }
    }

    private void outputResult(ResourceHandler handler, Keycloak client, String realm, String id) {
        if (returnId) {
            printOut(id);
        } else if (outputResult) {
            Object result = handler.get(client, realm, id);
            try {
                if (compressed) {
                    printOut(JsonSerialization.writeValueAsString(result));
                } else {
                    printOut(JsonSerialization.writeValueAsPrettyString(result));
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to print result as JSON " + result);
            }
        } else {
            printErr("Created new " + handler.getType().toTypeName() + " with id '" + id + "'");
        }
    }

    @Override
    protected boolean nothingToDo() {
        return noOptions() && file == null && (args == null || args.size() == 0);
    }

    protected String suggestHelp() {
        return EOL + "Try '" + CMD + " help create' for more information";
    }

    protected String help() {
        return usage();
    }

    public static String usage() {
        StringWriter sb = new StringWriter();
        PrintWriter out = new PrintWriter(sb);
        out.println("Usage: " + CMD + " create [ARGUMENTS]");
        out.println();
        out.println("Command to create new client configurations on the server. If Initial Access Token is specified (-t TOKEN)");
        out.println("or has previously been set for the server, and realm in the configuration ('" + CMD + " config initial-token'),");
        out.println("then that will be used, otherwise session access / refresh tokens will be used.");
        out.println();
        out.println("Arguments:");
        out.println();
        out.println("  Global options:");
        out.println("    -x                    Print full stack trace when exiting with error");
        out.println("    --config              Path to the config file (" + DEFAULT_CONFIG_FILE_STRING + " by default)");
        out.println("    --truststore PATH     Path to a truststore containing trusted certificates");
        out.println("    --trustpass PASSWORD  Truststore password (prompted for if not specified and --truststore is used)");
        out.println("    CREDENTIALS OPTIONS   Same set of options as accepted by '" + CMD + " config credentials' in order to establish");
        out.println("                          an authenticated sessions. This allows on-the-fly transient authentication that does");
        out.println("                          not touch a config file.");
        out.println();
        out.println("  Command specific options:");
        out.println("    -t, --token TOKEN     Use the specified Initial Access Token for authorization or read it from standard input ");
        out.println("                          if '-' is specified. This overrides any token set by '" + CMD + " config initial-token'.");
        out.println("                          If not specified, session credentials are used - see: CREDENTIALS OPTIONS.");
        out.println("    -e, --endpoint TYPE   Endpoint type / document format to use - one of: 'default', 'oidc', 'saml2'.");
        out.println("                          If not specified, the format is deduced from input file or falls back to 'default'.");
        out.println("    -s, --set NAME=VALUE  Set a specific attribute NAME to a specified value VALUE");
        out.println("    -f, --file FILENAME   Read object from file or standard input if FILENAME is set to '-'");
        out.println("    -o, --output          After creation output the new client configuration to standard output");
        out.println("    -c, --compressed      Don't pretty print the output");
        out.println("    -i, --clientId        After creation only print clientId to standard output");
        out.println();
        out.println("Examples:");
        out.println();
        out.println("Create a new client using configuration read from standard input:");
        if (OS_ARCH.isWindows()) {
            out.println("  " + PROMPT + " echo { \"clientId\": \"my_client\" } | " + CMD + " create -f -");
        } else {
            out.println("  " + PROMPT + " " + CMD + " create -f - << EOF");
            out.println("  {");
            out.println("    \"clientId\": \"my_client\"");
            out.println("  }");
            out.println("  EOF");
        }
        out.println();
        out.println("Since we didn't specify an endpoint type it will be deduced from configuration format.");
        out.println("Supported formats include Keycloak default format, OIDC format, and SAML SP Metadata.");
        out.println();
        out.println("Creating a client using file as a template, and overriding some attributes:");
        out.println("  " + PROMPT + " " + CMD + " create -f my_client.json -s clientId=my_client2 -s 'redirectUris=[\"http://localhost:8980/myapp/*\"]'");
        out.println();
        out.println("Creating a client using an Initial Access Token - you'll be prompted for a token:");
        out.println("  " + PROMPT + " " + CMD + " create -s clientId=my_client2 -s 'redirectUris=[\"http://localhost:8980/myapp/*\"]' -t -");
        out.println();
        out.println("Creating a client using 'oidc' endpoint. Without setting endpoint type here it would be 'default':");
        out.println("  " + PROMPT + " " + CMD + " create -e oidc -s 'redirect_uris=[\"http://localhost:8980/myapp/*\"]'");
        out.println();
        out.println("Creating a client using 'saml2' endpoint. In this case setting endpoint type is redundant since it is deduced ");
        out.println("from file content:");
        out.println("  " + PROMPT + " " + CMD + " create -e saml2 -f saml-sp-metadata.xml");
        out.println();
        out.println();
        out.println("Use '" + CMD + " help' for general information and a list of commands");
        return sb.toString();
    }

}
