package com.aichatbot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE = "aichatbot.json";
    private static final String ENV_FILE = "aichatbot.env";

    public String apiUrl = "http://localhost:4567/chat";
    public String apiToken = "YOUR_API_TOKEN";
    public String apiKey = "";
    public String model = "";
    public String systemPrompt ="";
    public String prefix = "!ai";
    public double temperature = 0.7;

    /**
     * 從設定檔載入設定，如果不存在則建立預設設定檔
     * 接著讀取 .env 檔案，覆蓋敏感設定（如 API Key）
     */
    public static ModConfig load() {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        Path configFile = configDir.resolve(CONFIG_FILE);

        ModConfig config;

        if (Files.exists(configFile)) {
            try (Reader reader = Files.newBufferedReader(configFile)) {
                config = GSON.fromJson(reader, ModConfig.class);
                AIChatBotMod.LOGGER.info("已載入設定檔: {}", configFile);
            } catch (Exception e) {
                AIChatBotMod.LOGGER.error("讀取設定檔失敗，使用預設設定", e);
                config = new ModConfig();
                config.save();
            }
        } else {
            config = new ModConfig();
            config.save();
        }

        // 讀取 .env 檔案覆蓋敏感設定
        config.loadEnv(configDir);

        return config;
    }

    /**
     * 從 .env 檔案讀取環境變數並覆蓋設定
     * 支援的變數：
     *   AICHATBOT_API_TOKEN   → apiToken (Ollama FastAPI)
     *   AICHATBOT_API_KEY    → apiKey (OpenRouter/OpenAI)
     *   AICHATBOT_API_URL    → apiUrl
     *   AICHATBOT_MODEL      → model
     *   AICHATBOT_PREFIX     → prefix
     *   AICHATBOT_TEMPERATURE → temperature
     */
    private void loadEnv(Path configDir) {
        Path envFile = configDir.resolve(ENV_FILE);

        if (!Files.exists(envFile)) {
            // 建立範本 .env 檔案
            createEnvTemplate(envFile);
            return;
        }

        Map<String, String> env = parseEnvFile(envFile);
        boolean changed = false;

        if (env.containsKey("AICHATBOT_API_TOKEN")) {
            this.apiToken = env.get("AICHATBOT_API_TOKEN");
            changed = true;
        }
        if (env.containsKey("AICHATBOT_API_KEY")) {
            this.apiKey = env.get("AICHATBOT_API_KEY");
            changed = true;
        }
        if (env.containsKey("AICHATBOT_API_URL")) {
            this.apiUrl = env.get("AICHATBOT_API_URL");
            changed = true;
        }
        if (env.containsKey("AICHATBOT_MODEL")) {
            this.model = env.get("AICHATBOT_MODEL");
            changed = true;
        }
        if (env.containsKey("AICHATBOT_PREFIX")) {
            this.prefix = env.get("AICHATBOT_PREFIX");
            changed = true;
        }
        if (env.containsKey("AICHATBOT_TEMPERATURE")) {
            try {
                this.temperature = Double.parseDouble(env.get("AICHATBOT_TEMPERATURE"));
                changed = true;
            } catch (NumberFormatException e) {
                AIChatBotMod.LOGGER.warn("AICHATBOT_TEMPERATURE 格式錯誤，使用預設值");
            }
        }

        if (changed) {
            AIChatBotMod.LOGGER.info("已從 .env 檔案載入環境變數: {}", envFile);
        }
    }

    /**
     * 解析 .env 檔案，格式為 KEY=VALUE（支援 # 開頭的註解）
     */
    private Map<String, String> parseEnvFile(Path envFile) {
        Map<String, String> env = new HashMap<>();
        try {
            for (String line : Files.readAllLines(envFile)) {
                line = line.trim();
                // 跳過空行和註解
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int eqIndex = line.indexOf('=');
                if (eqIndex > 0) {
                    String key = line.substring(0, eqIndex).trim();
                    String value = line.substring(eqIndex + 1).trim();
                    // 移除值的引號（支援 "value" 和 'value'）
                    if (value.length() >= 2) {
                        if ((value.startsWith("\"") && value.endsWith("\""))
                                || (value.startsWith("'") && value.endsWith("'"))) {
                            value = value.substring(1, value.length() - 1);
                        }
                    }
                    env.put(key, value);
                }
            }
        } catch (Exception e) {
            AIChatBotMod.LOGGER.error("讀取 .env 檔案失敗", e);
        }
        return env;
    }

    /**
     * 建立 .env 範本檔案
     */
    private void createEnvTemplate(Path envFile) {
        try (Writer writer = Files.newBufferedWriter(envFile)) {
            writer.write("# AI ChatBot 環境變數設定\n");
            writer.write("# 在這裡設定敏感資訊，避免直接寫在 aichatbot.json\n");
            writer.write("# .env 的值會覆蓋 aichatbot.json 的對應設定\n");
            writer.write("\n");
            writer.write("# Ollama FastAPI Token（必填）\n");
            writer.write("AICHATBOT_API_TOKEN=YOUR_API_TOKEN\n");
            writer.write("\n");
            writer.write("# API 端點 URL（選填，預設 http://localhost:4567/chat）\n");
            writer.write("# AICHATBOT_API_URL=http://localhost:4567/chat\n");
            writer.write("\n");
            writer.write("# OpenRouter/OpenAI API Key（如使用 OpenAI 才需要）\n");
            writer.write("# AICHATBOT_API_KEY=\n");
            writer.write("\n");
            writer.write("# 模型名稱（如使用 OpenAI 才需要）\n");
            writer.write("# AICHATBOT_MODEL=\n");
            writer.write("\n");
            writer.write("# 聊天觸發前綴（選填）\n");
            writer.write("# AICHATBOT_PREFIX=!ai\n");
            writer.write("\n");
            writer.write("# 回覆隨機程度 0.0~1.0（選填）\n");
            writer.write("# AICHATBOT_TEMPERATURE=0.7\n");
            AIChatBotMod.LOGGER.info("已建立 .env 範本檔案: {}", envFile);
        } catch (Exception e) {
            AIChatBotMod.LOGGER.error("建立 .env 範本檔案失敗", e);
        }
    }

    /**
     * 儲存設定至檔案
     */
    public void save() {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        Path configFile = configDir.resolve(CONFIG_FILE);

        try (Writer writer = Files.newBufferedWriter(configFile)) {
            GSON.toJson(this, writer);
            AIChatBotMod.LOGGER.info("已儲存設定檔: {}", configFile);
        } catch (Exception e) {
            AIChatBotMod.LOGGER.error("儲存設定檔失敗", e);
        }
    }
}
