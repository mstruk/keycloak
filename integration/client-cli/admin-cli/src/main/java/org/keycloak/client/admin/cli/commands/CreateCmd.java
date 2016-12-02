package org.keycloak.client.admin.cli.commands;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jboss.aesh.cl.Arguments;
import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import org.keycloak.client.admin.cli.common.AttributeOperation;
import org.keycloak.client.admin.cli.common.CmdStdinContext;
import org.keycloak.client.admin.cli.config.ConfigData;
import org.keycloak.client.admin.cli.util.AccessibleBufferOutputStream;
import org.keycloak.client.admin.cli.util.HeadersBody;
import org.keycloak.client.admin.cli.util.HeadersBodyStatus;
import org.keycloak.client.admin.cli.util.HttpUtil;
import org.keycloak.client.admin.cli.util.Pair;
import org.keycloak.client.admin.cli.util.ReturnFields;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import static org.keycloak.client.admin.cli.common.AttributeOperation.Type.SET;
import static org.keycloak.client.admin.cli.util.AuthUtil.ensureToken;
import static org.keycloak.client.admin.cli.util.ConfigUtil.DEFAULT_CONFIG_FILE_STRING;
import static org.keycloak.client.admin.cli.util.ConfigUtil.credentialsAvailable;
import static org.keycloak.client.admin.cli.util.ConfigUtil.loadConfig;
import static org.keycloak.client.admin.cli.util.HttpUtil.composeResourceUrl;
import static org.keycloak.client.admin.cli.util.IoUtil.copyStream;
import static org.keycloak.client.admin.cli.util.IoUtil.printErr;
import static org.keycloak.client.admin.cli.util.IoUtil.printOut;
import static org.keycloak.client.admin.cli.util.OsUtil.CMD;
import static org.keycloak.client.admin.cli.util.OsUtil.EOL;
import static org.keycloak.client.admin.cli.util.OsUtil.OS_ARCH;
import static org.keycloak.client.admin.cli.util.OsUtil.PROMPT;
import static org.keycloak.client.admin.cli.util.OutputUtil.MAPPER;
import static org.keycloak.client.admin.cli.util.ParseUtil.mergeAttributes;
import static org.keycloak.client.admin.cli.util.ParseUtil.parseFileOrStdin;
import static org.keycloak.client.admin.cli.util.ParseUtil.parseKeyVal;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
@CommandDefinition(name = "create", description = "Command to create new resources")
public class CreateCmd extends AbstractAuthOptionsCmd implements Command {

    @Option(shortName = 'a', name = "admin-root", description = "URL of Admin REST endpoint root - e.g. http://localhost:8080/auth/admin", hasValue = true)
    String adminRestRoot;

    @Option(shortName = 'p', name = "pretty", description = "Pretty print if response type is application/json - causes mismatch with Content-Length header", hasValue = false)
    boolean pretty;

    @Option(shortName = 'f', name = "file", description = "Read object from file or standard input if FILENAME is set to '-'", hasValue = true)
    String file;

    @Option(name = "fields", description = "A pattern specifying which attributes of JSON response body to actually display as result - causes mismatch with Content-Length header", hasValue = true)
    String fields;

    @Option(shortName = 'H', name = "print-headers", description = "Print response headers", hasValue = false)
    boolean printHeaders ;

    @Option(shortName = 'i', name = "id", description = "After creation only print id of created resource to standard output", hasValue = false)
    boolean returnId = false;

    @Option(shortName = 'o', name = "output", description = "After creation output the new resource to standard output", hasValue = false)
    boolean outputResult = false;

    @Option(shortName = 'c', name = "compressed", description = "Don't pretty print the output", hasValue = false)
    boolean compressed = false;

    //@OptionGroup(shortName = 's', name = "set", description = "Set attribute to the specified value")
    //Map<String, String> attributes = new LinkedHashMap<>();

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

        LinkedHashMap<String, Pair> headers = new LinkedHashMap<>();

        List<AttributeOperation> attrs = new LinkedList<>();

        String type = "post";
        String url = null;

        if (args == null || args.isEmpty()) {
            throw new IllegalArgumentException("URI not specified");
        }

        Iterator<String> it = args.iterator();

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
                case "-h":
                case "--header": {
                    requireValue(it, option);
                    String[] keyVal = parseKeyVal(it.next());
                    headers.put(keyVal[0].toLowerCase(), new Pair(keyVal[0], keyVal[1]));
                    break;
                }
                default: {
                    if (url == null) {
                        url = option;
                    } else {
                        throw new IllegalArgumentException("Illegal argument: " + option);
                    }
                }
            }
        }


        if (url == null) {
            throw new IllegalArgumentException("Resource URI not specified");
        }

        if (file == null && attrs.size() == 0) {
            throw new IllegalArgumentException("No file nor attribute values specified");
        }

        if (outputResult && returnId) {
            throw new IllegalArgumentException("Options -o and -i are mutually exclusive");
        }

        // see if Content-Type header is explicitly set to non-json value
        Pair ctype = headers.get("content-type");

        InputStream body = null;

        CmdStdinContext<ObjectNode> ctx = new CmdStdinContext<>();

        if (file != null) {
            if (ctype != null && !"application/json".equals(ctype.getValue())) {
                if ("-".equals(file)) {
                    body = System.in;
                } else {
                    try {
                        body = new BufferedInputStream(new FileInputStream(file));
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException("File not found: " + file);
                    }
                }
            } else {
                ctx = parseFileOrStdin(file);
            }
        }

        if (attrs.size() > 0) {
            if (body != null) {
                throw new RuntimeException("Can't set attributes on content of type other than application/json");
            }

            ctx = mergeAttributes(ctx, MAPPER.createObjectNode(), attrs);
        }

        if (body == null) {
            body = new ByteArrayInputStream(ctx.getContent().getBytes(Charset.forName("utf-8")));
        }

        ReturnFields returnFields = null;

        if (fields != null) {
            returnFields = new ReturnFields(fields);
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

        if (auth != null) {
            headers.put("Authorization", new Pair("Authorization", auth));
        }


        final String server = config.getServerUrl();
        final String realm = getTargetRealm(config);
        final String adminRoot = adminRestRoot != null ? adminRestRoot : composeAdminRoot(server);


        String resourceUrl = composeResourceUrl(adminRoot, realm, url);
        String typeName = extractTypeNameFromUri(resourceUrl);

        // make sure content type is set
        if (ctype == null) {
            ctype = new Pair("Content-Type", "application/json");
            headers.put(ctype.getKey(), ctype);
        }

        HeadersBodyStatus response;
        try {
            response = HttpUtil.doRequest(type, resourceUrl, new HeadersBody(new LinkedList<>(headers.values()), body));
        } catch (IOException e) {
            throw new RuntimeException("HTTP request error: " + e.getMessage(), e);
        }

        // output response
        if (printHeaders) {
            printOut(response.getStatus());
            for (Pair p : response.getHeaders()) {
                printOut(p.getKey() + ": " + p.getValue());
            }
        }

        response.checkSuccess();

        AccessibleBufferOutputStream abos = new AccessibleBufferOutputStream(System.out);
        if (response.getBody() == null) {
            throw new RuntimeException("Internal error - response body should never be null");
        }

        if (printHeaders) {
            printOut("");
        }


        String location = response.getHeader("Location");
        if (returnId && location != null) {
            printOut(extractLastComponentOfUri(location));
        } else if (pretty || returnFields != null) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            copyStream(response.getBody(), buffer);

            try {
                JsonNode rootNode = MAPPER.readValue(buffer.toByteArray(), JsonNode.class);
                if (returnFields != null) {
                    rootNode = applyFieldFilter(MAPPER, rootNode, returnFields);
                }
                // now pretty print it to output
                MAPPER.writeValue(abos, rootNode);
            } catch (Exception ignored) {
                copyStream(new ByteArrayInputStream(buffer.toByteArray()), abos);
            }
        } else {
            //copyStream(response.getBody(), abos);
            String id = location != null ? ("'" + extractLastComponentOfUri(location) + "'") : "unknown";
            printErr("Created new " + typeName + " with id " + id);
        }

        int lastByte = abos.getLastByte();
        if (lastByte != -1 && lastByte != 13 && lastByte != 10) {
            printErr("");
        }

        return CommandResult.SUCCESS;
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
