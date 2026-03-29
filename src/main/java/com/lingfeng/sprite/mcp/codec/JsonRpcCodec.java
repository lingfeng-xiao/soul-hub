package com.lingfeng.sprite.mcp.codec;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * JSON-RPC 2.0 encoding/decoding for MCP
 */
public class JsonRpcCodec {
    private static final Logger logger = LoggerFactory.getLogger(JsonRpcCodec.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    public static final String JSONRPC_VERSION = "2.0";

    // Request types
    public enum RequestType {
        INITIALIZE,
        TOOLS_LIST,
        TOOLS_CALL,
        RESOURCES_LIST,
        RESOURCES_READ,
        PROMPTS_LIST,
        PROMPTS_GET,
        CANCEL
    }

    /**
     * Encode a JSON-RPC request
     */
    public static String encodeRequest(String method, Object params, Object id) {
        try {
            ObjectNode node = mapper.createObjectNode();
            node.put("jsonrpc", JSONRPC_VERSION);
            node.put("method", method);
            if (params != null) {
                node.set("params", mapper.valueToTree(params));
            }
            if (id != null) {
                node.put("id", id.toString());
            }
            return mapper.writeValueAsString(node);
        } catch (Exception e) {
            logger.error("Failed to encode request", e);
            return encodeError(-32600, "Invalid Request", null);
        }
    }

    /**
     * Encode a JSON-RPC response with result
     */
    public static String encodeResponse(Object result, Object id) {
        try {
            ObjectNode node = mapper.createObjectNode();
            node.put("jsonrpc", JSONRPC_VERSION);
            node.set("result", mapper.valueToTree(result));
            if (id != null) {
                node.put("id", id.toString());
            }
            return mapper.writeValueAsString(node);
        } catch (Exception e) {
            logger.error("Failed to encode response", e);
            return encodeError(-32603, "Internal error", id);
        }
    }

    /**
     * Encode a JSON-RPC error response
     */
    public static String encodeError(int code, String message, Object id) {
        try {
            ObjectNode node = mapper.createObjectNode();
            node.put("jsonrpc", JSONRPC_VERSION);
            ObjectNode error = node.putObject("error");
            error.put("code", code);
            error.put("message", message);
            if (id != null) {
                node.put("id", id.toString());
            }
            return mapper.writeValueAsString(node);
        } catch (Exception e) {
            logger.error("Failed to encode error", e);
            return "{\"jsonrpc\":\"2.0\",\"error\":{\"code\":-32603,\"message\":\"Internal error\"}}";
        }
    }

    /**
     * Encode a JSON-RPC notification (no id)
     */
    public static String encodeNotification(String method, Object params) {
        try {
            ObjectNode node = mapper.createObjectNode();
            node.put("jsonrpc", JSONRPC_VERSION);
            node.put("method", method);
            if (params != null) {
                node.set("params", mapper.valueToTree(params));
            }
            return mapper.writeValueAsString(node);
        } catch (Exception e) {
            logger.error("Failed to encode notification", e);
            return encodeError(-32600, "Invalid Request", null);
        }
    }

    /**
     * Parse a JSON-RPC message
     */
    public static JsonRpcMessage parse(String json) {
        try {
            return mapper.readValue(json, JsonRpcMessage.class);
        } catch (Exception e) {
            logger.error("Failed to parse JSON-RPC message: {}", json, e);
            return JsonRpcMessage.error(-32600, "Parse error", null);
        }
    }

    /**
     * JSON-RPC message representation
     */
    public static class JsonRpcMessage {
        private final String jsonrpc;
        private final String method;
        private final Object params;
        private final ObjectNode result;
        private final JsonError error;
        private final Object id;

        public JsonRpcMessage(String jsonrpc, String method, Object params, ObjectNode result,
                             JsonError error, Object id) {
            this.jsonrpc = jsonrpc;
            this.method = method;
            this.params = params;
            this.result = result;
            this.error = error;
            this.id = id;
        }

        public static JsonRpcMessage request(String method, Object params, Object id) {
            return new JsonRpcMessage(JSONRPC_VERSION, method, params, null, null, id);
        }

        public static JsonRpcMessage response(ObjectNode result, Object id) {
            return new JsonRpcMessage(JSONRPC_VERSION, null, null, result, null, id);
        }

        public static JsonRpcMessage error(int code, String message, Object id) {
            return new JsonRpcMessage(JSONRPC_VERSION, null, null, null, new JsonError(code, message), id);
        }

        public static class JsonError {
            public final int code;
            public final String message;

            public JsonError(int code, String message) {
                this.code = code;
                this.message = message;
            }
        }

        // Getters
        public String getJsonrpc() { return jsonrpc; }
        public String getMethod() { return method; }
        public Object getParams() { return params; }
        public ObjectNode getResult() { return result; }
        public JsonError getError() { return error; }
        public Object getId() { return id; }

        public boolean isRequest() { return method != null; }
        public boolean isResponse() { return result != null || error != null; }
        public boolean isError() { return error != null; }
    }
}
