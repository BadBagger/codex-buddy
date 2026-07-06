package com.softsmith.codexbuddy;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

final class OpenAiChatClient {
    private static final String RESPONSES_URL = "https://api.openai.com/v1/responses";

    private final List<Message> history = new ArrayList<>();

    String send(String apiKey, String model, String userText) throws Exception {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return "Open Codex Buddy and add your OpenAI API key first.";
        }

        history.add(new Message("user", userText));

        JSONObject body = new JSONObject();
        body.put("model", model);
        body.put("instructions", "You are Codex Buddy on Android. Be concise, practical, and help the user with the app or screen they are currently using. Do not claim to control Android unless the user has enabled explicit permissions and the app has implemented that action.");
        JSONArray input = new JSONArray();
        int start = Math.max(0, history.size() - 12);
        for (int i = start; i < history.size(); i++) {
            Message message = history.get(i);
            JSONObject item = new JSONObject();
            item.put("role", message.role);
            item.put("content", message.content);
            input.put(item);
        }
        body.put("input", input);

        HttpURLConnection connection = (HttpURLConnection) new URL(RESPONSES_URL).openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(20000);
        connection.setReadTimeout(60000);
        connection.setDoOutput(true);
        connection.setRequestProperty("Authorization", "Bearer " + apiKey.trim());
        connection.setRequestProperty("Content-Type", "application/json");

        byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);
        try (OutputStream outputStream = connection.getOutputStream()) {
            outputStream.write(payload);
        }

        int code = connection.getResponseCode();
        String response = readAll(code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream());
        if (code < 200 || code >= 300) {
            return "OpenAI request failed (" + code + "): " + compactError(response);
        }

        String answer = extractOutputText(response);
        history.add(new Message("assistant", answer));
        return answer;
    }

    private static String readAll(InputStream inputStream) throws Exception {
        if (inputStream == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        return builder.toString();
    }

    private static String extractOutputText(String raw) throws Exception {
        JSONObject json = new JSONObject(raw);
        String outputText = json.optString("output_text", "");
        if (!outputText.isEmpty()) {
            return outputText.trim();
        }

        JSONArray output = json.optJSONArray("output");
        if (output == null) {
            return "I got a response, but it did not include text.";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < output.length(); i++) {
            JSONObject item = output.optJSONObject(i);
            if (item == null) {
                continue;
            }
            JSONArray content = item.optJSONArray("content");
            if (content == null) {
                continue;
            }
            for (int j = 0; j < content.length(); j++) {
                JSONObject contentItem = content.optJSONObject(j);
                if (contentItem != null) {
                    String text = contentItem.optString("text", "");
                    if (!text.isEmpty()) {
                        if (builder.length() > 0) {
                            builder.append("\n");
                        }
                        builder.append(text);
                    }
                }
            }
        }
        return builder.length() == 0 ? "I got a response, but it did not include text." : builder.toString().trim();
    }

    private static String compactError(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "No error body returned.";
        }
        try {
            JSONObject json = new JSONObject(raw);
            JSONObject error = json.optJSONObject("error");
            if (error != null) {
                String message = error.optString("message", raw);
                return message.length() > 240 ? message.substring(0, 240) + "..." : message;
            }
        } catch (Exception ignored) {
            // Fall back to raw body below.
        }
        return raw.length() > 240 ? raw.substring(0, 240) + "..." : raw;
    }

    private static final class Message {
        final String role;
        final String content;

        Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
}
