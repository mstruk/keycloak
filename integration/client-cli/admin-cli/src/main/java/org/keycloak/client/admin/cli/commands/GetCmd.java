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

import org.jboss.aesh.cl.Arguments;
import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.client.admin.cli.config.ConfigData;
import org.keycloak.client.admin.cli.util.OutputFormat;
import org.keycloak.client.admin.cli.util.ParseUtil;
import org.keycloak.client.admin.cli.util.ResourceType;
import org.keycloak.client.admin.cli.util.ReturnFields;
import org.keycloak.util.JsonSerialization;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.keycloak.client.admin.cli.util.AuthUtil.ensureToken;
import static org.keycloak.client.admin.cli.util.ConfigUtil.DEFAULT_CONFIG_FILE_STRING;
import static org.keycloak.client.admin.cli.util.ConfigUtil.credentialsAvailable;
import static org.keycloak.client.admin.cli.util.ConfigUtil.loadConfig;
import static org.keycloak.client.admin.cli.util.FilterUtil.copyFilteredObject;
import static org.keycloak.client.admin.cli.util.IoUtil.printOut;
import static org.keycloak.client.admin.cli.util.IoUtil.warnfErr;
import static org.keycloak.client.admin.cli.util.OsUtil.CMD;
import static org.keycloak.client.admin.cli.util.OsUtil.EOL;
import static org.keycloak.client.admin.cli.util.OsUtil.PROMPT;
import static org.keycloak.client.admin.cli.util.OutputUtil.printAsCsv;
import static org.keycloak.client.admin.cli.util.ParseUtil.checkResourceType;
import static org.keycloak.client.admin.cli.util.ParseUtil.parseKeyVal;


/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
@CommandDefinition(name = "get", description = "[ARGUMENTS]")
public class GetCmd extends AbstractAuthOptionsCmd {

    @Option(shortName = 'c', name = "compressed", description = "Print full stack trace when exiting with error", hasValue = false)
    private boolean compressed = false;

    //@Option(shortName = 'f', name = "filter", description = "Filter resource list by specified fields and their values", hasValue = false)
    //private List<String> queryFilter;

    @Option(name = "fields", description = "A pattern specifying which attributes of JSON response to actually display as result", hasValue = true)
    protected String fields;

    @Option(shortName = 'o', name = "offset", description = "Number of results from beginning of resultset to skip", hasValue = true)
    private int offset = 0;

    @Option(shortName = 'l', name = "limit", description = "Maksimum number of results to return", hasValue = true, defaultValue = "1000")
    private int limit = 1000;

    @Option(name = "format", description = "Output format - one of: json, csv", hasValue = true, defaultValue = "json")
    protected String format;

    @Arguments
    private List<String> args;

    @Override
    public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {

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

            String id = null;
            if (!resourceType.isCollectionType()) {
                if (!it.hasNext()) {
                    throw new IllegalArgumentException("Resource ID not specified");
                }
                id = it.next();

                if (id.startsWith("-")) {
                    warnfErr(ParseUtil.ID_OPTION_WARN, id);
                }
            }

            OutputFormat outputFormat;
            try {
                outputFormat = OutputFormat.valueOf(format.toUpperCase());
            } catch (Exception e) {
                throw new RuntimeException("Unsupported output format: " + format);
            }

            Map<String, String> filter = new HashMap<>();

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
                        throw new IllegalArgumentException("Invalid option: " + option);
                    }
                }
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

            final String server = config.getServerUrl();
            final String realm = config.getRealm();

            // Initialize admin client Keycloak object
            // delegate to resource type create method
            Keycloak client = KeycloakBuilder.builder()
                    .serverUrl(server)
                    .realm(realm)
                    .clientId(config.getRealmConfigData(server, realm).getClientId())
                    .authorization(auth)
                    .build();

            Object result;
            if (resourceType.isCollectionType()) {
                result = resourceType.getMany(client, realm, offset, limit, filter);
            } else {
                result = resourceType.get(client, realm, id);
            }

            if (returnFields != null) {
                try {
                    result = copyFilteredObject(result, returnFields);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to filter response", e);
                }
            }

            try {
                if (outputFormat == OutputFormat.JSON) {
                    if (compressed) {
                        printOut(JsonSerialization.writeValueAsString(result));
                    } else {
                        printOut(JsonSerialization.writeValueAsPrettyString(result));
                    }
                } else {
                    printAsCsv(result, returnFields);
                }

            } catch (Exception e) {
                throw new RuntimeException("Failed to print result as JSON " + result);
            }

            return CommandResult.SUCCESS;

        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(e.getMessage() + suggestHelp(), e);
        } finally {
            commandInvocation.stop();
        }
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
