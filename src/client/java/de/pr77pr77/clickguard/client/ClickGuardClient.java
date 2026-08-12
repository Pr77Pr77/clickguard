package de.pr77pr77.clickguard.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

import static de.pr77pr77.clickguard.ClickGuard.id;

public class ClickGuardClient implements ClientModInitializer {
    public static KeyMapping openPresetsScreen;
    public static KeyMapping enableClickingKey;

    public static boolean autoClickingEnabled;
    protected static boolean pendingDisconnect = false;
    public static AutoStoppedInfo autoStoppedInfo;

    public static ConfigManager configManager;

    public static List<Clicker> clickers = new ArrayList<>();

    @Override
    public void onInitializeClient() {
        KeyMapping.Category clickGuardKeybindCategory = new KeyMapping.Category(Identifier.fromNamespaceAndPath("clickguard", "clickguard"));
        openPresetsScreen = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.clickguard.openPresetsScreen",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_COMMA,
                clickGuardKeybindCategory
        ));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openPresetsScreen.consumeClick()) {
                client.execute(() -> {
                    if (client.gui.screen() == null) {
                        client.gui.setScreen(new PresetsScreen());
                    }
                });
            }
        });

        enableClickingKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.clickguard.toggleClicking",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_PERIOD,
                clickGuardKeybindCategory
        ));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (enableClickingKey.consumeClick()) {
                client.execute(() -> {
                    if (client.gui.screen() == null) {
                        toggleAutoClickingEnabled();
                    }
                });
            }
        });

        LevelRenderEvents.START_MAIN.register(_ -> {
            if (autoClickingEnabled || autoStoppedInfo != null) {
                for (Clicker clicker : clickers) {
                    if (autoClickingEnabled) {
                        clicker.handleAutomaticClicks();
                    }

                    AutoStoppedInfo info = clicker.checkActions();
                    if (info != null && autoStoppedInfo == null) {
                        autoStoppedInfo = info;
                        break;
                    }
                }
                if (autoStoppedInfo != null && autoClickingEnabled) {
                    toggleAutoClickingEnabled();
                }
            }
        });

        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (pendingDisconnect) {
                pendingDisconnect = false;
                client.disconnect(new TitleScreen(), false);
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((_, _) -> {
            // Reset the auto clickers after disconnect.
            autoClickingEnabled = false;
            for (Clicker clicker : clickers) {
                clicker.releaseClickIfClicking();
            }
            autoStoppedInfo = null;
            clickers.clear();
        });

        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                id("autoclicker_hud"),
                HUD::renderClickerHud
        );

        configManager = new ConfigManager();
    }

    public static void toggleAutoClickingEnabled() {
        autoClickingEnabled = !autoClickingEnabled;
        if (Minecraft.getInstance().gui.screen() instanceof PresetsScreen presetsScreen) {
            presetsScreen.startStopButton.setMessage(PresetsScreen.getStartStopButtonComponent());
            presetsScreen.list.updateEnableButtons();
        }

        if (autoClickingEnabled) {
            autoStoppedInfo = null;
            clickers.clear();
            for (ConfigManager.ConfigData.Preset preset : configManager.data.presets) {
                if (preset.enabled) {
                    clickers.add(new Clicker(preset));
                }
            }
        } else {
            for (Clicker clicker : clickers) {
                clicker.releaseClickIfClicking();
            }
            if (autoStoppedInfo == null) {
                clickers.clear();
            }
        }
    }

    public static class AutoStoppedInfo {
        final ConfigManager.ConfigData.Preset causingPreset;
        final Component reasonMessage;

        public AutoStoppedInfo(ConfigManager.ConfigData.Preset causingPreset, Component reasonMessage) {
            this.causingPreset = causingPreset;
            this.reasonMessage = reasonMessage;
        }
    }

    public static NarratableEntry NarratableEntryOfComponent(Component text) {
        if (text == null) {
            return null;
        }
        return new NarratableEntry() {
            @Override
            public NarratableEntry.@NonNull NarrationPriority narrationPriority() {
                return NarrationPriority.HOVERED;
            }

            @Override
            public void updateNarration(@NonNull NarrationElementOutput output) {
                output.add(NarratedElementType.TITLE, text);
            }
        };
    }
}