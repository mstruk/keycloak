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

package org.keycloak.client.admin.cli.util;

import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContexts;
import org.keycloak.util.JsonSerialization;

import javax.net.ssl.SSLContext;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.keycloak.client.admin.cli.util.IoUtil.printOut;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class HttpUtil {

    public static final String APPLICATION_XML = "application/xml";
    public static final String APPLICATION_JSON = "application/json";
    public static final String APPLICATION_FORM_URL_ENCODED = "application/x-www-form-urlencoded";
    public static final String UTF_8 = "utf-8";

    private static HttpClient httpClient;
    private static SSLConnectionSocketFactory sslsf;

    public static InputStream doGet(String url, String acceptType, String authorization) {
        try {
            HttpGet request = new HttpGet(url);
            request.setHeader(HttpHeaders.ACCEPT, acceptType);
            return doRequest(authorization, request);
        } catch (IOException e) {
            throw new RuntimeException("Failed to send request - " + e.getMessage(), e);
        }
    }

    public static InputStream doPost(String url, String contentType, String acceptType, String content, String authorization) {
        try {
            return doPostOrPut(contentType, acceptType, content, authorization, new HttpPost(url));
        } catch (IOException e) {
            throw new RuntimeException("Failed to send request - " + e.getMessage(), e);
        }
    }

    public static InputStream doPut(String url, String contentType, String acceptType, String content, String authorization) {
        try {
            return doPostOrPut(contentType, acceptType, content, authorization, new HttpPut(url));
        } catch (IOException e) {
            throw new RuntimeException("Failed to send request - " + e.getMessage(), e);
        }
    }

    public static void doDelete(String url, String authorization) {
        try {
            HttpDelete request = new HttpDelete(url);
            doRequest(authorization, request);
        } catch (IOException e) {
            throw new RuntimeException("Failed to send request - " + e.getMessage(), e);
        }
    }

    public static HeadersBodyStatus doRequest(String type, String url, HeadersBody request) throws IOException {
        HttpRequestBase req;
        switch (type) {
            case "get":
                req = new HttpGet(url);
                break;
            case "post":
                req = new HttpPost(url);
                break;
            case "put":
                req = new HttpPut(url);
                break;
            case "delete":
                req = new HttpDelete(url);
                break;
            case "options":
                req = new HttpOptions(url);
                break;
            case "head":
                req = new HttpHead(url);
                break;
            default:
                throw new RuntimeException("Method not supported: " + type);
        }
        addHeaders(req, request.getHeaders());

        if (request.getBody() != null) {
            if (req instanceof HttpEntityEnclosingRequestBase == false) {
                throw new RuntimeException("Request type does not support body: " + type);
            }
            ((HttpEntityEnclosingRequestBase) req).setEntity(new InputStreamEntity(request.getBody()));
        }

        HttpResponse res = getHttpClient().execute(req);
        InputStream responseStream = null;
        if (res.getEntity() != null) {
            responseStream = res.getEntity().getContent();
        } else {
            responseStream = new InputStream() {
                @Override
                public int read () throws IOException {
                    return -1;
                }
            };
        }

        List<Pair> headers = new ArrayList<>();
        HeaderIterator it = res.headerIterator();
        while (it.hasNext()) {
            Header header = it.nextHeader();
            headers.add(new Pair(header.getName(), header.getValue()));
        }

        return new HeadersBodyStatus(res.getStatusLine().toString(), headers, responseStream);
    }

    private static void addHeaders(HttpRequestBase request, List<Pair> headers) {
        for (Pair p: headers) {
            request.setHeader(p.getKey(), p.getValue());
        }
    }

    private static InputStream doPostOrPut(String contentType, String acceptType, String content, String authorization, HttpEntityEnclosingRequestBase request) throws IOException {
        request.setHeader(HttpHeaders.CONTENT_TYPE, contentType);
        request.setHeader(HttpHeaders.ACCEPT, acceptType);
        if (content != null) {
            request.setEntity(new StringEntity(content));
        }

        return doRequest(authorization, request);
    }

    private static InputStream doRequest(String authorization, HttpRequestBase request) throws IOException {
        addAuth(request, authorization);

        HttpResponse response = getHttpClient().execute(request);
        InputStream responseStream = null;
        if (response.getEntity() != null) {
            responseStream = response.getEntity().getContent();
        }

        int code = response.getStatusLine().getStatusCode();
        if (code >= 200 && code < 300) {
            return responseStream;
        } else {
            Map<String, String> error = null;
            try {
                Header header = response.getEntity().getContentType();
                if (header != null && APPLICATION_JSON.equals(header.getValue())) {
                    error = JsonSerialization.readValue(responseStream, Map.class);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to read error response - " + e.getMessage(), e);
            } finally {
                responseStream.close();
            }

            String message = null;
            if (error != null) {
                message = error.get("error_description") + " [" + error.get("error") + "]";
            }
            throw new RuntimeException(message != null ? message : response.getStatusLine().getStatusCode() + " " + response.getStatusLine().getReasonPhrase());
        }
    }

    private static void addAuth(HttpRequestBase request, String authorization) {
        if (authorization != null) {
            request.setHeader(HttpHeaders.AUTHORIZATION, authorization);
        }
    }

    public static HttpClient getHttpClient() {
        if (httpClient == null) {
            if (sslsf != null) {
                httpClient = HttpClientBuilder.create().useSystemProperties().setSSLSocketFactory(sslsf).build();
            } else {
                httpClient = HttpClientBuilder.create().useSystemProperties().build();
            }
        }
        return httpClient;
    }

    public static String urlencode(String value) {
        try {
            return URLEncoder.encode(value, UTF_8);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Failed to urlencode", e);
        }
    }

    public static void setTruststore(File file, String password) throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, KeyManagementException {
        if (!file.isFile()) {
            throw new RuntimeException("Truststore file not found: " + file.getAbsolutePath());
        }
        SSLContext theContext = SSLContexts.custom()
                .useProtocol("TLS")
                .loadTrustMaterial(file, password == null ? null : password.toCharArray())
                .build();
        sslsf = new SSLConnectionSocketFactory(theContext);
    }

    public static String extractIdFromLocation(String location) {
        int last = location.lastIndexOf("/");
        if (last != -1) {
            return location.substring(last + 1);
        }
        return null;
    }

    public static void checkStatusCreated(Response response) {
        checkStatus(response, Response.Status.CREATED.getStatusCode());
    }

    public static void checkStatusNoContent(Response response) {
        checkStatus(response, Response.Status.NO_CONTENT.getStatusCode());
    }

    public static void checkStatus(Response response, int expected) {
        if (response.getStatus() != expected) {
            Object body = response.getEntity();
            String err = "Error: " + response.getStatus() + " " + response.getStatusInfo();
            if (body instanceof Map) {
                Map<String, String> msg = (Map<String, String>) body;
                String errmsg = msg.get("error_description");
                if (errmsg != null) {
                    err += " - " + errmsg + "[" + msg.get("error") + "]";
                }
            }
            throw new RuntimeException(err);
        }
    }
}
