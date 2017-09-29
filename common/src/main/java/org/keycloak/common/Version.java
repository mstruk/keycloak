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

package org.keycloak.common;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class Version {
    public static final String UNKNOWN = "UNKNOWN";

    private static final Builder builder = new Builder();

    public static final String NAME = builder.getName();
    public static final String NAME_HTML = builder.getNameHtml();
    public static final String VERSION = builder.getVersion();
    public static final String RESOURCES_VERSION = builder.getResourcesVersion();
    public static final String BUILD_TIME = builder.getBuildTime();
    public static final String DEFAULT_PROFILE = builder.getDefaultProfile();
    public static final boolean IS_COMMUNITY_VERSION = builder.isCommunityVersion();


    private static Logger log = Logger.getLogger(Version.class.getName());


    private static class Builder {

        private Properties p;

        private Builder() {
            Properties props = new Properties();
            InputStream is = Version.class.getResourceAsStream("/keycloak-version.properties");
            try {
                props.load(is);
            } catch(IOException e) {
                props = null;
                log.log(Level.WARNING, "Failed to load keycloak-version.properties", e);
            }
            p = props;
        }

        String getName() {
            return getProperty("name", null);
        }

        String getNameHtml() {
            return getProperty("name-html", null);
        }

        String getDefaultProfile() {
            return getProperty("default-profile", null);
        }

        String getVersion() {
            return getProperty("version", UNKNOWN);
        }

        String getBuildTime() {
            String buildTimeOverride = System.getProperty("keycloak.version.buildtime");
            if (buildTimeOverride != null) {
                return buildTimeOverride;
            }
            return getProperty("build-time", UNKNOWN);
        }

        String getProperty(String name, String defaultValue) {
            if (p == null) {
                return defaultValue;
            }
            return p.getProperty(name);
        }

        String getResourcesVersion() {
            if (p == null) {
                return null;
            }
            String v = getVersion().toLowerCase();
            if (v.endsWith("-snapshot")) {
                v = v.replace("-snapshot", "-" + getBuildTime().replace(" ", "").replace(":", "").replace("-", ""));
            }
            return v;
        }

        boolean isCommunityVersion() {
            return "Keycloak".equals(getVersion());
        }
    }

}
