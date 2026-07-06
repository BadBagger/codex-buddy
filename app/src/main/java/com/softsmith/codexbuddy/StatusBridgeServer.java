package com.softsmith.codexbuddy;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

final class StatusBridgeServer {
    interface Listener {
        void onStatusEvent(String title, String message, String status);
    }

    static final int PORT = 8787;

    private final Listener listener;
    private volatile boolean running;
    private ServerSocket serverSocket;
    private Thread thread;

    StatusBridgeServer(Listener listener) {
        this.listener = listener;
    }

    void start() {
        if (running) {
            return;
        }
        running = true;
        thread = new Thread(this::runLoop, "codex-buddy-status-server");
        thread.start();
    }

    void stop() {
        running = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (Exception ignored) {
        }
    }

    private void runLoop() {
        try {
            serverSocket = new ServerSocket(PORT);
            while (running) {
                Socket socket = serverSocket.accept();
                handle(socket);
            }
        } catch (Exception ignored) {
            if (running) {
                listener.onStatusEvent("Codex Buddy", "Status listener stopped unexpectedly.", "error");
            }
        }
    }

    private void handle(Socket socket) {
        try (Socket closeable = socket;
             BufferedReader reader = new BufferedReader(new InputStreamReader(closeable.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(closeable.getOutputStream(), StandardCharsets.UTF_8))) {
            String requestLine = reader.readLine();
            if (requestLine == null) {
                return;
            }
            int contentLength = 0;
            String line;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                String lower = line.toLowerCase(Locale.US);
                if (lower.startsWith("content-length:")) {
                    contentLength = Integer.parseInt(line.substring(line.indexOf(':') + 1).trim());
                }
            }

            String body = "";
            if (contentLength > 0) {
                char[] chars = new char[contentLength];
                int read = reader.read(chars);
                if (read > 0) {
                    body = new String(chars, 0, read);
                }
            }

            if (requestLine.startsWith("GET /health")) {
                writeResponse(writer, 200, "{\"ok\":true}");
                return;
            }
            if (!requestLine.startsWith("POST /notify")) {
                writeResponse(writer, 404, "{\"ok\":false}");
                return;
            }

            JSONObject json = body.trim().isEmpty() ? new JSONObject() : new JSONObject(body);
            String title = json.optString("title", "Codex update");
            String message = json.optString("message", "Codex status changed.");
            String status = json.optString("status", "done");
            listener.onStatusEvent(title, message, status);
            writeResponse(writer, 200, "{\"ok\":true}");
        } catch (Exception ignored) {
        }
    }

    private void writeResponse(BufferedWriter writer, int code, String body) throws Exception {
        writer.write("HTTP/1.1 " + code + " OK\r\n");
        writer.write("Content-Type: application/json\r\n");
        writer.write("Content-Length: " + body.getBytes(StandardCharsets.UTF_8).length + "\r\n");
        writer.write("Connection: close\r\n\r\n");
        writer.write(body);
        writer.flush();
    }
}
