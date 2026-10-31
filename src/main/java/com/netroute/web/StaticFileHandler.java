package com.netroute.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Serves static frontend assets (HTML, CSS, JS, SVG, etc.) securely.
 * Checks both classpath resources and filesystem directories (supporting both development and JAR execution).
 */
public class StaticFileHandler implements HttpHandler {

    private static final String CLASSPATH_PREFIX = "/static";
    private static final Path FILESYSTEM_DEV_PATH = Paths.get("src", "main", "resources", "static");

    private static final Map<String, String> MIME_TYPES = Map.ofEntries(
            Map.entry(".html", "text/html; charset=UTF-8"),
            Map.entry(".htm", "text/html; charset=UTF-8"),
            Map.entry(".css", "text/css; charset=UTF-8"),
            Map.entry(".js", "application/javascript; charset=UTF-8"),
            Map.entry(".mjs", "application/javascript; charset=UTF-8"),
            Map.entry(".json", "application/json; charset=UTF-8"),
            Map.entry(".svg", "image/svg+xml"),
            Map.entry(".png", "image/png"),
            Map.entry(".jpg", "image/jpeg"),
            Map.entry(".jpeg", "image/jpeg"),
            Map.entry(".gif", "image/gif"),
            Map.entry(".ico", "image/x-icon"),
            Map.entry(".woff", "font/woff"),
            Map.entry(".woff2", "font/woff2"),
            Map.entry(".ttf", "font/ttf"),
            Map.entry(".txt", "text/plain; charset=UTF-8")
    );

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        HttpUtils.addCorsHeaders(exchange);

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }

        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod()) && !"HEAD".equalsIgnoreCase(exchange.getRequestMethod())) {
            HttpUtils.sendError(exchange, 405, "Method Not Allowed");
            return;
        }

        String rawPath = exchange.getRequestURI().getPath();
        if (rawPath == null || rawPath.isBlank() || "/".equals(rawPath)) {
            rawPath = "/index.html";
        }

        // Security check: reject directory traversal attempts
        if (rawPath.contains("..") || rawPath.contains("//") || rawPath.contains("\\") || rawPath.contains("\0")) {
            HttpUtils.sendError(exchange, 400, "Invalid resource path");
            return;
        }

        byte[] content = loadResourceBytes(rawPath);
        if (content == null) {
            // Fallback for single page app routing if not found and not an asset file
            if (!rawPath.contains(".")) {
                content = loadResourceBytes("/index.html");
                rawPath = "/index.html";
            }
        }

        if (content == null) {
            HttpUtils.sendError(exchange, 404, "Static resource not found: " + rawPath);
            return;
        }

        String mime = getMimeType(rawPath);
        exchange.getResponseHeaders().set("Content-Type", mime);
        exchange.getResponseHeaders().set("Cache-Control", "no-cache, must-revalidate");
        exchange.sendResponseHeaders(200, content.length);

        if (!"HEAD".equalsIgnoreCase(exchange.getRequestMethod())) {
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(content);
            }
        }
    }

    private byte[] loadResourceBytes(String normalizedPath) {
        if (!normalizedPath.startsWith("/")) {
            normalizedPath = "/" + normalizedPath;
        }

        // 1. Try filesystem dev path first (allows hot editing of html/css/js without rebuild)
        Path fsPath = FILESYSTEM_DEV_PATH.resolve(normalizedPath.substring(1)).normalize();
        if (fsPath.startsWith(FILESYSTEM_DEV_PATH) && java.nio.file.Files.isRegularFile(fsPath)) {
            try {
                return java.nio.file.Files.readAllBytes(fsPath);
            } catch (IOException ignored) {}
        }

        // 2. Try classpath resource
        String cpPath = CLASSPATH_PREFIX + normalizedPath;
        try (InputStream is = getClass().getResourceAsStream(cpPath)) {
            if (is != null) {
                return is.readAllBytes();
            }
        } catch (IOException ignored) {}

        return null;
    }

    private String getMimeType(String path) {
        int dot = path.lastIndexOf('.');
        if (dot >= 0) {
            String ext = path.substring(dot).toLowerCase();
            return MIME_TYPES.getOrDefault(ext, "application/octet-stream");
        }
        return "text/html; charset=UTF-8";
    }
}
