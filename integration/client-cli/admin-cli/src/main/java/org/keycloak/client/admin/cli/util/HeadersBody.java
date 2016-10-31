package org.keycloak.client.admin.cli.util;

import java.io.InputStream;
import java.util.List;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class HeadersBody {

    private List<Pair> headers;
    private InputStream body;

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
}
