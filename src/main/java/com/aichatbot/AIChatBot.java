package com.aichatbot;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class AIChatBot {
    private final ModConfig config;

    public AIChatBot(ModConfig config) {
        this.config = config;
    }

    /**
     * 呼叫 AI API 取得回應
     *
     * @param prompt     使用者的提問內容
     * @param playerName 發問玩家的名稱（用於替換 {player}）
     * @return AI 的回應文字
     */
    public String callAI(String prompt, String playerName) {
        try {
            // 將 system prompt 中的 {player} 替換成實際玩家名稱，附加到 prompt 前面
            String commandContext = config.systemPrompt.replace("{player}", playerName);
            String fullPrompt = commandContext + "\n\n玩家 " + playerName + " 說: " + prompt;

            URL url = new URL(config.apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(60000);

            // 設定認證：Ollama FastAPI 用 X-API-TOKEN，OpenAI 用 Bearer
            if (!config.apiToken.isEmpty() && !config.apiToken.equals("YOUR_API_TOKEN")) {
                conn.setRequestProperty("X-API-TOKEN", config.apiToken);
            } else if (!config.apiKey.isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + config.apiKey);
            }

            // Ollama FastAPI 格式: {"prompt": "..."}
            String jsonBody = "{\"prompt\": \"" + escapeJson(fullPrompt) + "\"}";

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonBody.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), "utf-8"))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line.trim());
                    }
                    // Ollama FastAPI 回應格式: {"answer": "..."}
                    return parseAnswer(response.toString());
                }
            } else {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getErrorStream(), "utf-8"))) {
                    StringBuilder errorResponse = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        errorResponse.append(line.trim());
                    }
                    AIChatBotMod.LOGGER.error("API 錯誤 ({}): {}", responseCode, errorResponse);
                }
                return "§cAPI 請求失敗，錯誤代碼: " + responseCode;
            }
        } catch (Exception e) {
            AIChatBotMod.LOGGER.error("呼叫 AI API 時發生錯誤", e);
            return "§c連線發生錯誤: " + e.getMessage();
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * 從 Ollama FastAPI JSON 回應中解析 answer 欄位
     */
    private String parseAnswer(String jsonResponse) {
        // 尋找 "answer": " 或 "answer":"
        String marker = "\"answer\":";
        int idx = jsonResponse.indexOf(marker);
        if (idx == -1) return jsonResponse;

        idx += marker.length();

        // 跳過空白
        while (idx < jsonResponse.length() && jsonResponse.charAt(idx) == ' ') {
            idx++;
        }

        if (idx >= jsonResponse.length() || jsonResponse.charAt(idx) != '"') {
            return jsonResponse;
        }
        idx++; // 跳過開頭的 "

        StringBuilder content = new StringBuilder();
        boolean escaped = false;

        for (int i = idx; i < jsonResponse.length(); i++) {
            char c = jsonResponse.charAt(i);
            if (escaped) {
                switch (c) {
                    case 'n': content.append('\n'); break;
                    case 'r': content.append('\r'); break;
                    case 't': content.append('\t'); break;
                    case '"': content.append('"'); break;
                    case '\\': content.append('\\'); break;
                    default: content.append(c); break;
                }
                escaped = false;
            } else {
                if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    break; // 結束引號
                } else {
                    content.append(c);
                }
            }
        }
        return content.toString();
    }
}
