package de.pr77pr77.clickguard.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class ClickGuardClient implements ClientModInitializer {
    public static KeyMapping OpenCommandScreen;

    public static boolean autoClickingEnabled;

    public static ConfigManager configManager;

    public static List<Clicker> clickers = new ArrayList<>();

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

        LevelRenderEvents.START_MAIN.register(_ -> {
            if (autoClickingEnabled) {
                for (Clicker clicker : clickers) {
                    clicker.handleAutomaticClicks();
                }
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

        if (autoClickingEnabled) {
            clickers.clear();
            for (ConfigManager.ConfigData.Preset preset : configManager.data.presets) {
                if (preset.enabled) {
                    clickers.add(new Clicker(preset));
                }
            }
            // TODO: Block manual clicks
        } else {
            for (Clicker clicker : clickers) {
                clicker.releaseClickIfClicking();
            }
            clickers.clear();
        }
    }
}