package com.aichatbot;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AIChatBotMod implements ModInitializer {
    public static final String MOD_ID = "aichatbot";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private AIChatBot bot;
    private ModConfig config;

    @Override
    public void onInitialize() {
        LOGGER.info("AI ChatBot Mod 正在初始化...");

        // 載入設定檔
        config = ModConfig.load();
        bot = new AIChatBot(config);

        // 監聽玩家聊天訊息
        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            String content = message.getContent().getString();
            String playerName = sender.getName().getString();
            String prefix = config.prefix;

            // 檢查是否以設定的前綴開頭
            if (!content.startsWith(prefix + " ") && !content.equals(prefix)) {
                return;
            }

            // 擷取提問內容
            String prompt;
            if (content.equals(prefix)) {
                return; // 只打了前綴，沒有內容
            }
            prompt = content.substring(prefix.length() + 1).trim();

            if (prompt.isEmpty()) {
                return;
            }

            MinecraftServer server = sender.getServer();
            LOGGER.info("{} 正在詢問 AI: {}", playerName, prompt);

            // 通知玩家 AI 正在思考
            server.execute(() -> {
                sender.sendMessage(Text.literal("§e[AI] §7正在思考..."), false);
            });

            // 在新線程中呼叫 API，避免阻塞伺服器主線程
            new Thread(() -> {
                String response = bot.callAI(prompt);

                // 回到主線程處理回應
                server.execute(() -> {
                    if (response.startsWith("/")) {
                        // AI 回應是一個指令 → 執行它
                        String command = response.substring(1);
                        try {
                            server.getCommandManager().getDispatcher()
                                    .execute(command, server.getCommandSource());

                            // 通知所有玩家 AI 執行了指令
                            server.getPlayerManager().broadcast(
                                    Text.literal("§b[AI] §f執行指令: §a/" + command),
                                    false
                            );
                            LOGGER.info("AI 執行指令: /{}", command);
                        } catch (CommandSyntaxException e) {
                            server.getPlayerManager().broadcast(
                                    Text.literal("§c[AI] §f指令執行失敗: " + e.getMessage()),
                                    false
                            );
                            LOGGER.error("AI 指令執行失敗: {}", e.getMessage());
                        }
                    } else {
                        // 一般聊天回覆 → 廣播給所有玩家
                        server.getPlayerManager().broadcast(
                                Text.literal("§b[AI] §f" + response),
                                false
                        );
                    }
                });
            }, "AIChatBot-API-Thread").start();
        });

        LOGGER.info("AI ChatBot Mod 初始化完成！在聊天輸入 {} <訊息> 即可使用", config.prefix);
    }
}
