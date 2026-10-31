package com.netroute.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.netroute.web.dto.ApiResponse;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class HttpUtils {

    public static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private HttpUtils() {}

    public static void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, PATCH");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization, Accept");
    }

    public static boolean handlePreflight(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return true;
        }
        return false;
    }

    public static void sendJson(HttpExchange exchange, int statusCode, Object body) throws IOException {
        addCorsHeaders(exchange);
        byte[] bytes = GSON.toJson(body).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    public static <T> void sendSuccess(HttpExchange exchange, T data) throws IOException {
        sendJson(exchange, 200, ApiResponse.ok(data));
    }

    public static <T> void sendSuccess(HttpExchange exchange, String message, T data) throws IOException {
        sendJson(exchange, 200, ApiResponse.ok(message, data));
    }

    public static void sendError(HttpExchange exchange, int statusCode, String errorMessage) throws IOException {
        sendJson(exchange, statusCode, ApiResponse.failure(errorMessage));
    }

    public static String readBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    public static <T> T readJson(HttpExchange exchange, Class<T> clazz) throws IOException, JsonSyntaxException {
        String body = readBody(exchange);
        if (body == null || body.isBlank()) {
            return null;
        }
        return GSON.fromJson(body, clazz);
    }

    public static Map<String, String> parseQueryParams(HttpExchange exchange) {
        Map<String, String> result = new HashMap<>();
        String query = exchange.getRequestURI().getRawQuery();
        if (query == null || query.isBlank()) {
            return result;
        }
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf('=');
            if (idx > 0) {
                String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                String value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                result.put(key, value);
            } else if (!pair.isBlank()) {
                result.put(URLDecoder.decode(pair, StandardCharsets.UTF_8), "");
            }
        }
        return result;
    }
}
