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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jboss.aesh.cl.Arguments;
import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.keycloak.client.admin.cli.util.AuthUtil.ensureToken;
import static org.keycloak.client.admin.cli.util.ConfigUtil.DEFAULT_CONFIG_FILE_STRING;
import static org.keycloak.client.admin.cli.util.ConfigUtil.credentialsAvailable;
import static org.keycloak.client.admin.cli.util.ConfigUtil.loadConfig;
import static org.keycloak.client.admin.cli.util.IoUtil.copyStream;
import static org.keycloak.client.admin.cli.util.IoUtil.printErr;
import static org.keycloak.client.admin.cli.util.IoUtil.printOut;
import static org.keycloak.client.admin.cli.util.OsUtil.CMD;
import static org.keycloak.client.admin.cli.util.OsUtil.EOL;
import static org.keycloak.client.admin.cli.util.ParseUtil.parseKeyVal;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
@CommandDefinition(name = "rest", description = "TYPE URL [ARGUMENTS]")
public class RestCmd extends AbstractAuthOptionsCmd implements Command {

    @Option(shortName = 'a', name = "auth", description = "Add Authorization header with a fresh token", hasValue = false)
    protected boolean authorize;

    @Option(shortName = 'p', name = "pretty", description = "Pretty print if response type is application/json - causes mismatch with Content-Length header", hasValue = false)
    protected boolean pretty;

    @Option(shortName = 'f', name = "file", description = "Read object from file or standard input if FILENAME is set to '-'", hasValue = true)
    protected String file;

    @Option(name = "fields", description = "A pattern specifying which attributes of JSON response body to actually display as result - causes mismatch with Content-Length header", hasValue = true)
    protected String fields;

    @Option(shortName = 'r', name = "return-head", description = "Print response headers", hasValue = false)
    protected boolean printHeaders ;

    @Option(shortName = 'i', name = "id", description = "After creation print created resource id to standard output", hasValue = false)
    protected boolean returnId = false;

    @Arguments
    private List<String> args;

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

        if (args == null || args.isEmpty()) {
            throw new IllegalArgumentException("Request TYPE not specified");
        }

        String type = null;
        String url = null;

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
                    default: {
                        if (type == null) {
                            type = option;
                        } else if (url == null) {
                            url = option;
                        } else {
                            throw new IllegalArgumentException("Unsupported option: " + option);
                        }
                    }
                }
            }
        }

        if (url == null) {
            throw new IllegalArgumentException("URL not specified");
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

        ReturnFields returnFields = null;

        if (fields != null) {
            returnFields = new ReturnFields(fields);
        }

        ConfigData config = loadConfig();
        config = copyWithServerInfo(config);

        if (authorize) {
            String auth = null;
            config = ensureAuthInfo(config, commandInvocation);
            config = copyWithServerInfo(config);
            if (credentialsAvailable(config)) {
                auth = ensureToken(config);
            }
            if (auth != null) {
                headers.add(new Pair("Authorization", "Bearer " + auth));
            }
        }

        if (!url.startsWith("http")) {
            url = config.getServerUrl() + url;
        }

        setupTruststore(config, commandInvocation);

        HeadersBodyStatus response;
        try {
            response = HttpUtil.doRequest(type, url, new HeadersBody(headers, body));
        } catch (IOException e) {
            throw new RuntimeException("HTTP request error: " + e.getMessage(), e);
        }

        // output response
        if (printHeaders) {
            printOut(response.getStatus());
            for (Pair pair : response.getHeaders()) {
                printOut(pair.getKey() + ": " + pair.getValue());
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
            int last = location.lastIndexOf("/");
            if (last != -1) {
                printOut(location.substring(last + 1));
            }
        } else if (pretty || returnFields != null) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            copyStream(response.getBody(), buffer);

            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            try {
                JsonNode rootNode = mapper.readValue(buffer.toByteArray(), JsonNode.class);
                if (returnFields != null) {
                    rootNode = applyFieldFilter(mapper, rootNode, returnFields);
                }
                // now pretty print it to output
                mapper.writeValue(abos, rootNode);
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

    private JsonNode applyFieldFilter(ObjectMapper mapper, JsonNode rootNode, ReturnFields returnFields) {
        // construct new JsonNode that satisfies filtering specified by returnFields
        return copyFilteredObject(mapper, rootNode, returnFields);
    }

    private JsonNode copyFilteredObject(ObjectMapper mapper, JsonNode node, ReturnFields returnFields) {
        JsonNode r = node;
        if (node.isArray()) {
            ArrayNode ar = mapper.createArrayNode();
            for (JsonNode item: node) {
                ar.add(copyFilteredObject(mapper, item, returnFields));
            }
            r = ar;
        } else if (node.isObject()){
            r = mapper.createObjectNode();
            Iterator<String> fieldNames = node.fieldNames();
            while (fieldNames.hasNext()) {
                String name = fieldNames.next();
                if (returnFields.included(name)) {
                    JsonNode value = copyFilteredObject(mapper, node.get(name), returnFields.child(name));
                    ((ObjectNode) r).set(name, value);
                }
            }
        }
        return r;
    }

    private void requireValue(Iterator<String> it, String option) {
        if (!it.hasNext()) {
            throw new IllegalArgumentException("Option " + option + " requires a value");
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
        out.println("Usage: " + CMD + " rest TYPE URL [ARGUMENTS]");
        out.println();
        out.println("Command to perform raw HTTP requests for probing the server or for performing operations that have no");
        out.println("dedicated sub-command implemented yet. This command can make use of current authenticated session or on-the-fly");
        out.println("authentication. In addition, any HTTP headers can be set, and arbitrary content sent to the server");
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
        out.println("    TYPE                  HTTP method. One of: 'get', 'post', 'put', 'delete', 'options', 'head'");
        out.println("    URL                   Endpoint URL of HTTP request. If relative, current session --server is used");
        out.println("                          to construct an absolute url of the form: SERVER + '/' + URL");
        out.println("    -a, --auth            Use current authenticated session's access token");
        out.println("    -h, --head NAME=VALUE Add request header NAME with value VALUE (header with same NAME will be added multiple times)");
        out.println("    -f, --file FILENAME   Read request body from file or standard input if FILENAME is set to '-'");
        out.println("    -p, --pretty          Pretty print response body if response type is application/json - causes length mismatch");
        out.println("                          with Content-Length header");
        out.println("    -r, --return-head     Output response headers");
        out.println();
        out.println("Use '" + CMD + " help' for general information and a list of commands.");
        return sb.toString();
    }
}
