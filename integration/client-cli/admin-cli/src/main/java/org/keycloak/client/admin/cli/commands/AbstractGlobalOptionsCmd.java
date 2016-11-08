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

import org.jboss.aesh.cl.Option;
import org.jboss.aesh.console.command.Command;
import org.keycloak.client.admin.cli.aesh.Globals;

import static org.keycloak.client.admin.cli.util.IoUtil.printOut;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public abstract class AbstractGlobalOptionsCmd implements Command {

    @Option(shortName = 'x', description = "Print full stack trace when exiting with error", hasValue = false)
    protected boolean dumpTrace;

    @Option(name = "help", description = "Print command specific help", hasValue = false)
    protected boolean help;

    protected void initFromParent(AbstractGlobalOptionsCmd parent) {
        dumpTrace = parent.dumpTrace;
        help = parent.help;
    }

    protected void processGlobalOptions() {
        Globals.dumpTrace = dumpTrace;
    }

    protected boolean printHelp() {
        if (help || nothingToDo()) {
            printOut(help());
            return true;
        }

        return false;
    }

    protected boolean nothingToDo() {
        return false;
    }

    protected String help() {
        return KcAdmCmd.usage();
    }
}
