package org.keycloak.client.admin.cli.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.keycloak.util.JsonSerialization;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import static org.keycloak.client.admin.cli.util.IoUtil.printOut;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class OutputUtil {

    static ObjectMapper MAPPER = new ObjectMapper();

    static {
        MAPPER.enable(SerializationFeature.INDENT_OUTPUT);
        MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    public static JsonNode convertToJsonNode(Object object) throws IOException {
        if (object instanceof JsonNode) {
            return (JsonNode) object;
        }

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        buffer.write(JsonSerialization.writeValueAsBytes(object));
        return MAPPER.readValue(buffer.toByteArray(), JsonNode.class);
    }


    public static void printAsCsv(Object object, ReturnFields fields) throws IOException {

        JsonNode node = convertToJsonNode(object);
        if (!node.isArray()) {
            ArrayNode listNode = MAPPER.createArrayNode();
            listNode.add(node);
            node = listNode;
        }

        for (JsonNode item: node) {
            StringBuilder buffer = new StringBuilder();
            printObjectAsCsv(buffer, item, fields);

            printOut(buffer.length() > 0 ? buffer.substring(1) : "");
        }
    }

    static void printObjectAsCsv(StringBuilder out, JsonNode node) {
        printObjectAsCsv(out, node, null);
    }

    static void printObjectAsCsv(StringBuilder out, JsonNode node, ReturnFields fields) {

        if (node.isObject()) {
            if (fields == null) {
                Iterator<Map.Entry<String, JsonNode>> it = node.fields();
                while (it.hasNext()) {
                    printObjectAsCsv(out, it.next().getValue());
                }
            } else {
                Iterator<String> it = fields.iterator();
                while (it.hasNext()) {
                    String field = it.next();
                    JsonNode attr = node.get(field);
                    printObjectAsCsv(out, attr, fields.child(field));
                }
            }
        } else if (node.isArray()) {
            for (JsonNode item: node) {
                printObjectAsCsv(out, item, fields);
            }
        } else if (node != null) {
            out.append(",");
            out.append(node.toString());
        }
    }
}
