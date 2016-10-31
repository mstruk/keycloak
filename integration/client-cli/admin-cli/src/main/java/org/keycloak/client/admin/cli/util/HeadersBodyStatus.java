package org.keycloak.client.admin.cli.util;

import java.io.InputStream;
import java.util.List;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class HeadersBodyStatus extends HeadersBody {

    private final String status;

    public HeadersBodyStatus(String status, List<Pair> headers, InputStream body) {
        super(headers, body);
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
