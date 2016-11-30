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
import org.jboss.aesh.cl.Arguments;
import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import org.keycloak.client.admin.cli.config.ConfigData;
import org.keycloak.client.admin.cli.util.AccessibleBufferOutputStream;
import org.keycloak.client.admin.cli.util.HeadersBody;
import org.keycloak.client.admin.cli.util.HeadersBodyStatus;
import org.keycloak.client.admin.cli.util.HttpUtil;
import org.keycloak.client.admin.cli.util.OutputFormat;
import org.keycloak.client.admin.cli.util.Pair;
import org.keycloak.client.admin.cli.util.ReturnFields;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.keycloak.client.admin.cli.util.AuthUtil.ensureToken;
import static org.keycloak.client.admin.cli.util.ConfigUtil.DEFAULT_CONFIG_FILE_STRING;
import static org.keycloak.client.admin.cli.util.ConfigUtil.credentialsAvailable;
import static org.keycloak.client.admin.cli.util.ConfigUtil.loadConfig;
import static org.keycloak.client.admin.cli.util.IoUtil.copyStream;
import static org.keycloak.client.admin.cli.util.IoUtil.printErr;
import static org.keycloak.client.admin.cli.util.IoUtil.printOut;
import static org.keycloak.client.admin.cli.util.OsUtil.CMD;
import static org.keycloak.client.admin.cli.util.OsUtil.EOL;
import static org.keycloak.client.admin.cli.util.OsUtil.PROMPT;
import static org.keycloak.client.admin.cli.util.OutputUtil.MAPPER;
import static org.keycloak.client.admin.cli.util.OutputUtil.printAsCsv;
import static org.keycloak.client.admin.cli.util.ParseUtil.parseKeyVal;


/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
@CommandDefinition(name = "get", description = "[ARGUMENTS]")
public class GetCmd extends AbstractAuthOptionsCmd {

    @Option(shortName = 'a', name = "admin-root", description = "URL of Admin REST endpoint root - e.g. http://localhost:8080/auth/admin", hasValue = true)
    String adminRestRoot;

    @Option(shortName = 'p', name = "pretty", description = "Pretty print if response type is application/json - causes mismatch with Content-Length header", hasValue = false, defaultValue = "true")
    boolean pretty;

    @Option(shortName = 'f', name = "file", description = "Read object from file or standard input if FILENAME is set to '-'", hasValue = true)
    String file;

    @Option(name = "fields", description = "A pattern specifying which attributes of JSON response body to actually display as result - causes mismatch with Content-Length header", hasValue = true)
    String fields;

    @Option(shortName = 'H', name = "print-headers", description = "Print response headers", hasValue = false)
    boolean printHeaders ;

    @Option(shortName = 'c', name = "compressed", description = "Don't pretty print the output", hasValue = false)
    boolean compressed = false;

    @Option(shortName = 'o', name = "offset", description = "Number of results from beginning of resultset to skip", hasValue = true)
    int offset = 0;

    @Option(shortName = 'l', name = "limit", description = "Maksimum number of results to return", hasValue = true, defaultValue = "1000")
    int limit = 1000;

    @Option(name = "format", description = "Output format - one of: json, csv", hasValue = true, defaultValue = "json")
    String format;

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

        String type = "get";
        String url = null;

        if (args == null || args.isEmpty()) {
            throw new IllegalArgumentException("URI not specified");
        }

        OutputFormat outputFormat;
        try {
            outputFormat = OutputFormat.valueOf(format.toUpperCase());
        } catch (Exception e) {
            throw new RuntimeException("Unsupported output format: " + format);
        }

        Map<String, String> filter = new HashMap<>();

        Iterator<String> it = args.iterator();

        while (it.hasNext()) {
            String option = it.next();
            switch (option) {
                case "-f":
                case "--filter": {
                    if (!it.hasNext()) {
                        throw new IllegalArgumentException("Option " + option + " requires a value");
                    }
                    String arg = it.next();
                    String[] keyVal;
                    if (arg.indexOf("=") == -1) {
                        keyVal = new String[] {"", arg};
                    } else {
                        keyVal = parseKeyVal(arg);
                    }
                    filter.put(keyVal[0], keyVal[1]);
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

        HeadersBodyStatus response;
        try {
            response = HttpUtil.doRequest(type, resourceUrl, new HeadersBody(new LinkedList<>(headers.values())));
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
        if (pretty || returnFields != null) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            copyStream(response.getBody(), buffer);

            try {
                JsonNode rootNode = MAPPER.readValue(buffer.toByteArray(), JsonNode.class);
                if (returnFields != null) {
                    rootNode = applyFieldFilter(MAPPER, rootNode, returnFields);
                }
                if (outputFormat == OutputFormat.JSON) {
                    // now pretty print it to output
                    MAPPER.writeValue(abos, rootNode);
                } else {
                    printAsCsv(rootNode, returnFields);
                }
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
        return noOptions() && (args == null || args.size() == 0);
    }

    protected String suggestHelp() {
        return EOL + "Try '" + CMD + " help get' for more information";
    }

    protected String help() {
        return usage();
    }

    public static String usage() {
        StringWriter sb = new StringWriter();
        PrintWriter out = new PrintWriter(sb);
        out.println("Usage: " + CMD + " get CLIENT [ARGUMENTS]");
        out.println();
        out.println("Command to retrieve a client configuration description for a specified client. If registration access token");
        out.println("is specified or is available in configuration file, then it is used. Otherwise, current active session is used.");
        out.println();
        out.println("Arguments:");
        out.println();
        out.println("  Global options:");
        out.println("    -x                    Print full stack trace when exiting with error");
        out.println("    --config              Path to the config file (" + DEFAULT_CONFIG_FILE_STRING + " by default)");
        out.println("    --truststore PATH     Path to a truststore containing trusted certificates");
        out.println("    --trustpass PASSWORD  Truststore password (prompted for if not specified and --truststore is used)");
        out.println("    -t, --token TOKEN     Registration access token to use");
        out.println("    CREDENTIALS OPTIONS   Same set of options as accepted by '" + CMD + " config credentials' in order to establish");
        out.println("                          an authenticated sessions. This allows on-the-fly transient authentication that does");
        out.println("                          not touch a config file.");
        out.println();
        out.println("  Command specific options:");
        out.println("    CLIENT                ClientId of the client to display");
        out.println("    -c, --compressed      Don't pretty print the output");
        out.println("    -e, --endpoint TYPE   Endpoint type to use - one of: 'default', 'oidc', 'install'");
        out.println();
        out.println("Examples:");
        out.println();
        out.println("Get configuration in default format:");
        out.println("  " + PROMPT + " " + CMD + " get my_client");
        out.println();
        out.println("Get configuration in OIDC format:");
        out.println("  " + PROMPT + " " + CMD + " get my_client -e oidc");
        out.println();
        out.println("Get adapter configuration for the client:");
        out.println("  " + PROMPT + " " + CMD + " get my_client -e install");
        out.println();
        out.println();
        out.println("Use '" + CMD + " help' for general information and a list of commands");
        return sb.toString();
    }
}
