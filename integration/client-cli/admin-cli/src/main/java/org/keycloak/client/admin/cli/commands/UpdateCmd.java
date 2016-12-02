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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jboss.aesh.cl.Arguments;
import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.cl.Option;
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
import org.keycloak.client.admin.cli.util.ReflectionUtil;
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

import static org.keycloak.client.admin.cli.common.AttributeOperation.Type.DELETE;
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
import static org.keycloak.client.admin.cli.util.OsUtil.PROMPT;
import static org.keycloak.client.admin.cli.util.OutputUtil.MAPPER;
import static org.keycloak.client.admin.cli.util.ParseUtil.mergeAttributes;
import static org.keycloak.client.admin.cli.util.ParseUtil.parseFileOrStdin;
import static org.keycloak.client.admin.cli.util.ParseUtil.parseKeyVal;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
@CommandDefinition(name = "update", description = "CLIENT_ID [ARGUMENTS]")
public class UpdateCmd extends AbstractAuthOptionsCmd {

    @Option(shortName = 'a', name = "admin-root", description = "URL of Admin REST endpoint root - e.g. http://localhost:8080/auth/admin", hasValue = true)
    String adminRestRoot;

    @Option(shortName = 'p', name = "pretty", description = "Pretty print if response type is application/json - causes mismatch with Content-Length header", hasValue = false)
    boolean pretty;

    @Option(shortName = 'f', name = "file", description = "Read object from file or standard input if FILENAME is set to '-'", hasValue = true)
    String file;

    @Option(name = "fields", description = "A pattern specifying which attributes of JSON response body to actually display as result - causes mismatch with Content-Length header", hasValue = true)
    String fields;

    @Option(shortName = 'H', name = "print-headers", description = "Print response headers", hasValue = false)
    boolean printHeaders;

    @Option(shortName = 'm', name = "merge", description = "Merge new values with existing configuration on the server", hasValue = false)
    boolean mergeMode = true;

    @Option(shortName = 'o', name = "output", description = "After update output the new client configuration", hasValue = false)
    boolean outputResult = false;

    @Option(shortName = 'c', name = "compressed", description = "Don't pretty print the output", hasValue = false)
    boolean compressed = false;

    //@GroupOption(shortName = 's', name = "set", description = "Set specific attribute to a specified value", hasValue = true)
    //private List<String> attributes = new ArrayList<>();

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

        String type = "put";
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
                case "-d":
                case "--delete": {
                    attrs.add(new AttributeOperation(DELETE, it.next()));
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

        if (file == null && attrs.size() > 0) {
            mergeMode = true;
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

        // initialize config only after reading from stdin,
        // to allow proper operation when piping 'get' - which consumes the old
        // registration access token, and saves the new one to the config
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

        if (mergeMode) {
            ObjectNode result;
            HeadersBodyStatus response;
            try {
                response = HttpUtil.doRequest("get", resourceUrl, new HeadersBody(new LinkedList<>(headers.values())));
                response.checkSuccess();

                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                copyStream(response.getBody(), buffer);

                result = MAPPER.readValue(buffer.toByteArray(), ObjectNode.class);

            } catch (IOException e) {
                throw new RuntimeException("HTTP request error: " + e.getMessage(), e);
            }

            CmdStdinContext<ObjectNode> ctxremote = new CmdStdinContext<>();
            ctxremote.setResult(result);

            // merge local representation over remote one
            if (ctx.getResult() != null) {
                ReflectionUtil.merge(ctx.getResult(), ctxremote.getResult());
            }
            ctx = ctxremote;
        }

        if (attrs.size() > 0) {
            if (body != null) {
                throw new RuntimeException("Can't set attributes on content of type other than application/json");
            }
            ctx = mergeAttributes(ctx, MAPPER.createObjectNode(), attrs);
        }

        ReturnFields returnFields = null;

        if (fields != null) {
            returnFields = new ReturnFields(fields);
        }

        // make sure content type is set
        if (ctype == null) {
            ctype = new Pair("Content-Type", "application/json");
            headers.put(ctype.getKey(), ctype);
        }

        if (body == null) {
            body = new ByteArrayInputStream(ctx.getContent().getBytes(Charset.forName("utf-8")));
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

        if (pretty || returnFields != null) {
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
            copyStream(response.getBody(), abos);
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
        return EOL + "Try '" + CMD + " help update' for more information";
    }

    protected String help() {
        return usage();
    }

    public static String usage() {
        StringWriter sb = new StringWriter();
        PrintWriter out = new PrintWriter(sb);
        out.println("Usage: " + CMD + " update CLIENT [ARGUMENTS]");
        out.println();
        out.println("Command to update an existing client configuration. If registration access token is specified it is used.");
        out.println("Otherwise, if 'registrationAccessToken' attribute is set, that is used. Otherwise, if registration access");
        out.println("token is available in configuration file, we use that. Finally, if it's not available anywhere, the current ");
        out.println("active session is used.");
        out.println();
        out.println("Arguments:");
        out.println();
        out.println("  Global options:");
        out.println("    -x                    Print full stack trace when exiting with error");
        out.println("    --config              Path to the config file (" + DEFAULT_CONFIG_FILE_STRING + " by default)");
        out.println("    --truststore PATH     Path to a truststore containing trusted certificates");
        out.println("    --trustpass PASSWORD  Truststore password (prompted for if not specified and --truststore is used)");
        out.println("    --token TOKEN         Registration access token to use");
        out.println("    CREDENTIALS OPTIONS   Same set of options as accepted by '" + CMD + " config credentials' in order to establish");
        out.println("                          an authenticated sessions. This allows on-the-fly transient authentication that does");
        out.println("                          not touch a config file.");
        out.println();
        out.println("  Command specific options:");
        out.println("    CLIENT                ClientId of the client to update");
        out.println("    -s, --set KEY=VALUE   Set specific attribute to a specified value");
        out.println("              KEY+=VALUE  Add item to an array");
        out.println("    -d, --delete NAME     Delete the specific attribute, or array item");
        out.println("    -e, --endpoint TYPE   Endpoint type to use - one of: 'default', 'oidc'");
        out.println("    -f, --file FILENAME   Use the file or standard input if '-' is specified");
        out.println("    -m, --merge           Merge new values with existing configuration on the server");
        out.println("                          Merge is automatically enabled unless --file is specified");
        out.println("    -o, --output          After update output the new client configuration");
        out.println("    -c, --compressed      Don't pretty print the output");
        out.println();
        out.println();
        out.println("Nested attributes are supported by using '.' to separate components of a KEY. Optionaly, the KEY components ");
        out.println("can be quoted with double quotes - e.g. my_client.attributes.\"external.user.id\". If VALUE starts with [ and ");
        out.println("ends with ] the attribute will be set as a JSON array. If VALUE starts with { and ends with } the attribute ");
        out.println("will be set as a JSON object. If KEY ends with an array index - e.g. clients[3]=VALUE - then the specified item");
        out.println("of the array is updated. If KEY+=VALUE syntax is used, then KEY is assumed to be an array, and another item is");
        out.println("added to it.");
        out.println();
        out.println("Attributes can also be deleted. If KEY ends with an array index, then the targeted item of an array is removed");
        out.println("and the following items are shifted.");
        out.println();
        out.println("Merged mode fetches current configuration from the server, applies attribute changes to it, and sends it");
        out.println("back to the server, overwriting existing configuration there. If available, Registration Access Token is used ");
        out.println("for authorization when doing changes. Otherwise current session's authorization is used in which case user needs");
        out.println("manage-clients permission for update to work.");
        out.println();
        out.println();
        out.println("Examples:");
        out.println();
        out.println("Update a client by fetching current configuration from server, and applying specified changes.");
        out.println("  " + PROMPT + " " + CMD + " update my_client -s enabled=true -s 'redirectUris=[\"http://localhost:8080/myapp/*\"]'");
        out.println();
        out.println("Update a client by overwriting existing configuration on the server with a new one:");
        out.println("  " + PROMPT + " " + CMD + " update my_client -f new_my_client.json");
        out.println();
        out.println("Update a client by overwriting existing configuration using local file as a template:");
        out.println("  " + PROMPT + " " + CMD + " update my_client -f new_my_client.json -s enabled=true");
        out.println();
        out.println("Update client by fetching current configuration from server and merging with specified changes:");
        out.println("  " + PROMPT + " " + CMD + " update my_client -f new_my_client.json -s enabled=true --merge");
        out.println();
        out.println("Update a client using 'oidc' endpoint:");
        out.println("  " + PROMPT + " " + CMD + " update my_client -e oidc -s 'redirect_uris=[\"http://localhost:8080/myapp/*\"]'");
        out.println();
        out.println();
        out.println("Use '" + CMD + " help' for general information and a list of commands");
        return sb.toString();
    }
}
