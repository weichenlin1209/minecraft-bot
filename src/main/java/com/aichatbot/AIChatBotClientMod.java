package com.aichatbot;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class AIChatBotClientMod implements ClientModInitializer {
    private static KeyBinding openInputKey;
    private static final String PREFIX = "!ai ";

    @Override
    public void onInitializeClient() {
        openInputKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.aichatbot.open_input",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                "category.aichatbot"
        ));

        AIChatBotMod.LOGGER.info("AIChatBotClientMod: registered keybinding 'key.aichatbot.open_input' (K)");

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null) {
            mc.execute(() -> {
                if (mc.inGameHud != null) {
                    mc.inGameHud.getChatHud().addMessage(Text.of("[AI] 按 K 開啟 AI 輸入"));
                }
            });
        }

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (openInputKey.wasPressed()) {
                AIChatBotMod.LOGGER.info("AIChatBotClientMod: open key pressed");
                client.execute(() -> {
                    client.setScreen(new ChatScreen(PREFIX));
                });
            }
        });
    }
}
