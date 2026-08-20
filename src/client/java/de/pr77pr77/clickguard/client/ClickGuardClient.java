package de.pr77pr77.clickguard.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

import static de.pr77pr77.clickguard.ClickGuard.id;

public class ClickGuardClient implements ClientModInitializer {
    public static final int TEXT_GRAY = 0xFFE0E0E0;

    public static KeyMapping openPresetsScreen;
    public static KeyMapping enableClickingKey;

    public static boolean autoClickingEnabled;
    public static AutoDisconnectInfo pendingDisconnect;
    public static AutoStoppedInfo autoStoppedInfo;

    public static ConfigManager configManager;

    public static List<Clicker> clickers = new ArrayList<>();

    @Override
    public void onInitializeClient() {
        KeyMapping.Category clickGuardKeybindCategory = new KeyMapping.Category(ResourceLocation.fromNamespaceAndPath("clickguard", "clickguard"));
        openPresetsScreen = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.clickguard.openPresetsScreen",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_COMMA,
                clickGuardKeybindCategory
        ));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openPresetsScreen.consumeClick()) {
                client.execute(() -> {
                    if (client.screen == null) {
                        client.setScreen(new PresetsScreen());
                    }
                });
            }
        });

        enableClickingKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.clickguard.toggleClicking",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_PERIOD,
                clickGuardKeybindCategory
        ));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (enableClickingKey.consumeClick()) {
                client.execute(() -> {
                    if (client.screen == null) {
                        toggleAutoClickingEnabled();
                    }
                });
            }
        });

        WorldRenderEvents.START_MAIN.register(context -> {
            if (autoClickingEnabled || autoStoppedInfo != null) {
                for (Clicker clicker : clickers) {
                    if (autoClickingEnabled) {
                        clicker.handleAutomaticClicks();
                    }

                    if (!(Minecraft.getInstance().screen instanceof EditPresetScreen) && !(Minecraft.getInstance().screen instanceof PresetsScreen)) {
                        AutoStoppedInfo info = clicker.checkActions();
                        if (info != null && autoStoppedInfo == null) {
                            autoStoppedInfo = info;
                            break;
                        }
                    }
                }
                if (autoStoppedInfo != null && autoClickingEnabled) {
                    toggleAutoClickingEnabled();
                }
            }
        });

        ClientTickEvents.START_CLIENT_TICK.register(minecraft -> {
            if (pendingDisconnect != null) {
                LocalPlayer player = Minecraft.getInstance().player;
                if (player != null) {
                    pendingDisconnect.health = player.getHealth();
                    pendingDisconnect.hunger = player.getFoodData().getFoodLevel();
                }
                minecraft.disconnect(new AutoDisconnectScreen(pendingDisconnect), false);
                pendingDisconnect = null;
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((listener, minecraft) -> {
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
        if (Minecraft.getInstance().screen instanceof PresetsScreen presetsScreen) {
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

    public static class AutoDisconnectInfo {
        final ConfigManager.ConfigData.Preset causingPreset;
        final ConfigManager.ConfigData.SimpleAction causingAction;
        float health;
        int hunger;

        public AutoDisconnectInfo(ConfigManager.ConfigData.Preset causingPreset, ConfigManager.ConfigData.SimpleAction causingAction) {
            this.causingPreset = causingPreset;
            this.causingAction = causingAction;
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

    public static Component formatDuration(long milliseconds) {
        long hours = milliseconds / 3_600_000;
        long minutes = (milliseconds % 3_600_000) / 60_000;
        long seconds = (milliseconds % 60_000) / 1000;
        long millis = milliseconds % 1000;

        List<Component> parts = new ArrayList<>();

        if (hours > 0) {
            parts.add(Component.translatable("clickguard.hours", hours));
        }
        if (minutes > 0) {
            parts.add(Component.translatable("clickguard.minutes", minutes));
        }
        if (seconds > 0) {
            parts.add(Component.translatable("clickguard.seconds", seconds));
        }
        if (millis > 0 || parts.isEmpty()) {
            parts.add(Component.translatable("clickguard.milliseconds", millis));
        }

        MutableComponent result = Component.empty();
        for (int i = 0; i < parts.size(); i++) {
            result.append(parts.get(i));
            if (i < parts.size() - 1) {
                result.append(", ");
            }
        }

        return result;
    }

    public static NarratableEntry NarratableEntryOfComponent(Component text) {
        if (text == null) {
            return null;
        }
        return new NarratableEntry() {
            @Override
            public NarratableEntry.@NotNull NarrationPriority narrationPriority() {
                return NarrationPriority.HOVERED;
            }

            @Override
            public void updateNarration(@NotNull NarrationElementOutput output) {
                output.add(NarratedElementType.TITLE, text);
            }
        };
    }
}