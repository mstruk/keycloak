package org.keycloak.example.utils;

import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.common.util.KeycloakUriBuilder;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author <a href="mailto:marko.strukelj@gmail.com">Marko Strukelj</a>
 */
public class ServletUtils {

    public static String getLogoutUrl(HttpServletRequest request) {
        return getLogoutUrl(request, null);
    }

    public static String getLogoutUrl(HttpServletRequest request, String redirectUri) {
        KeycloakSecurityContext ctx = (KeycloakSecurityContext) request.getAttribute(KeycloakSecurityContext.class.getName());

        // can we expect ctx to always be non-null?

        KeycloakDeployment deployment = ((RefreshableKeycloakSecurityContext) ctx).getDeployment();
        // TODO: deployment.getLogoutUrl() should already return cloned instance
        KeycloakUriBuilder logoutUrl = deployment.getLogoutUrl().clone();
        URI uri;
        try {
            uri = new URI(request.getRequestURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Assertion failed - HttpServletRequest object returned invalid RequestURI: ", e);
        }

        String kscheme = logoutUrl.getScheme();
        String khost = logoutUrl.getHost();
        int kport = logoutUrl.getPort();
        if (("https".equals(kscheme) && kport == 443)
            || "http".equals(kscheme) && kport == 80) {
            kport = -1;
        }

        URI localUri;
        try {
            localUri = new URI(request.getRequestURL().toString());
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Assertion failed - HttpServletRequest object returned invalid RequestURL: ", e);
        }
        String scheme = localUri.getScheme();
        String host = localUri.getHost();
        int port = localUri.getPort();
        if (("https".equals(scheme) && port == 443)
            || "http".equals(scheme) && port == 80) {
            port = -1;
        }

        String redirect = null;

        if (redirectUri != null) {
            try {
                localUri = new URI(redirectUri);
            } catch (URISyntaxException e) {
                throw new IllegalStateException("Assertion failed - invalid redirect uri passed: ", e);
            }

            if (localUri.getHost() == null) {
                uri = localUri;
            } else {
                redirect = localUri.toString();
            }
        }

        if (redirect == null) {
            if (scheme.equals(kscheme) && host.equals(khost) && port == kport) {
                redirect = uri.toString();
            } else {
                redirect = scheme + "://" + host + (port != -1 ? ":" + port: "") + uri.toString();
            }
        }
        logoutUrl.queryParam("redirect_uri", redirect);
        return logoutUrl.build("").toString();
    }


    public static String getAccountUrl(HttpServletRequest request) {
        return getAccountUrl(request, null);
    }

    public static String getAccountUrl(HttpServletRequest request, String redirectUri) {
        KeycloakSecurityContext ctx = (KeycloakSecurityContext) request.getAttribute(KeycloakSecurityContext.class.getName());

        // can we expect ctx to always be non-null?
        KeycloakDeployment deployment = ((RefreshableKeycloakSecurityContext) ctx).getDeployment();

        KeycloakUriBuilder accountUrl = KeycloakUriBuilder.fromUri(deployment.getAccountUrl());

        URI uri;
        try {
            uri = new URI(request.getRequestURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Assertion failed - HttpServletRequest object returned invalid RequestURI: ", e);
        }

        String kscheme = accountUrl.getScheme();
        String khost = accountUrl.getHost();
        int kport = accountUrl.getPort();
        if (("https".equals(kscheme) && kport == 443)
            || "http".equals(kscheme) && kport == 80) {
            kport = -1;
        }

        URI localUri;
        try {
            localUri = new URI(request.getRequestURL().toString());
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Assertion failed - HttpServletRequest object returned invalid RequestURL: ", e);
        }
        String scheme = localUri.getScheme();
        String host = localUri.getHost();
        int port = localUri.getPort();
        if (("https".equals(scheme) && port == 443)
            || "http".equals(scheme) && port == 80) {
            port = -1;
        }

        String redirect = null;

        if (redirectUri != null) {
            try {
                localUri = new URI(redirectUri);
            } catch (URISyntaxException e) {
                throw new IllegalStateException("Assertion failed - invalid redirect uri passed: ", e);
            }

            if (localUri.getHost() == null) {
                uri = localUri;
            } else {
                redirect = localUri.toString();
            }
        }

        if (redirect == null) {
            if (scheme.equals(kscheme) && host.equals(khost) && port == kport) {
                redirect = uri.toString();
            } else {
                redirect = scheme + "://" + host + (port != -1 ? ":" + port: "") + uri.toString();
            }
        }
        accountUrl.queryParam("referrer", redirect);
        return accountUrl.build("").toString();
    }
}
