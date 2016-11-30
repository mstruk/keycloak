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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;

import static org.keycloak.client.admin.cli.util.IoUtil.copyStream;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class HeadersBody {

    private List<Pair> headers;
    private InputStream body;


    public HeadersBody(List<Pair> headers) {
        this.headers = headers;
    }

    public HeadersBody(List<Pair> headers, InputStream body) {
        this.headers = headers;
        this.body = body;
    }

    public List<Pair> getHeaders() {
        return headers;
    }

    public InputStream getBody() {
        return body;
    }

    public String bodyString() {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        copyStream(getBody(), os);
        return new String(os.toByteArray(), Charset.forName(getContentCharset()));
    }

    public String getHeader(String name) {
        for (Pair pair: headers) {
            if (pair.getKey().toLowerCase().equals(name.toLowerCase())) {
                return pair.getValue();
            }
        }
        return null;
    }

    public String getContentCharset() {
        String contentType = getHeader("Content-Type");
        if (contentType != null) {
            int pos = contentType.lastIndexOf("charset=");
            if (pos != -1) {
                return contentType.substring(pos + 8);
            }
        }
        return "iso-8859-1";
    }


}
