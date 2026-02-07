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
            // 將 system prompt 中的 {player} 替換成實際玩家名稱
            String systemPrompt = config.systemPrompt.replace("{player}", playerName);

            URL url = new URL(config.apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + config.apiKey);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);

            String jsonBody = "{"
                    + "\"model\": \"" + escapeJson(config.model) + "\","
                    + "\"messages\": ["
                    + "  {\"role\": \"system\", \"content\": \"" + escapeJson(systemPrompt) + "\"},"
                    + "  {\"role\": \"user\", \"content\": \"" + escapeJson(prompt) + "\"}"
                    + "],"
                    + "\"temperature\": " + config.temperature
                    + "}";

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
                    return parseContent(response.toString());
                }
            } else {
                // 讀取錯誤回應以獲得更多資訊
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
     * 從 OpenAI JSON 回應中解析 content 欄位
     */
    private String parseContent(String jsonResponse) {
        // 尋找 "content": " 或 "content":"
        String marker = "\"content\":";
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
