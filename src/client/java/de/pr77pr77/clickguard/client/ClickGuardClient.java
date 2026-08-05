package de.pr77pr77.clickguard.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class ClickGuardClient implements ClientModInitializer {
    public static KeyMapping OpenCommandScreen;

    public static boolean autoClickingEnabled;

    public static ConfigManager configManager;

    @Override
    public void onInitializeClient() {
        KeyMapping.Category SavedcommandsKeyindCategory = new KeyMapping.Category(Identifier.fromNamespaceAndPath("clickguard", "clickguard"));
        OpenCommandScreen = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.clickguard.openPresetsScreen",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_COMMA,
                SavedcommandsKeyindCategory
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OpenCommandScreen.consumeClick()) {
                client.execute(() -> {
                    if (client.gui.screen() == null) {
                        client.gui.setScreen(new PresetsScreen());
                    }
                });
            }
        });

        configManager = new ConfigManager();
    }

    public static void toggleAutoClickingEnabled() {
        autoClickingEnabled = !autoClickingEnabled;
        if (Minecraft.getInstance().gui.screen() instanceof PresetsScreen presetsScreen) {
            presetsScreen.startStopButton.setMessage(PresetsScreen.getStartStopButtonComponent());
            presetsScreen.list.updateEnableButtons();
        }

        // TODO!
    }
}