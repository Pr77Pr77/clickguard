package de.pr77pr77.clickguard.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import net.minecraft.util.Util;

import static de.pr77pr77.clickguard.client.ClickGuardClient.*;

public class HUD {
    public static void renderClickerHud(GuiGraphicsExtractor graphics, DeltaTracker tickCounter) {
        if (!autoClickingEnabled) {
            return;
        }

        Font font = Minecraft.getInstance().font;
        int x = 5;
        int y = 5;
        int lineHeight = 10;

        graphics.text(font, Component.translatable("clickguard.hud.title"), x, y, CommonColors.TEXT_GRAY);
        y += lineHeight;

        for (Clicker clicker : clickers) {
            Component text = switch (clicker.preset.clickingType) {
                case CUSTOM_TIMING -> Component.translatable("clickguard.hud.customTiming",
                        clicker.preset.name, Component.translatable(clicker.preset.keybind.getName()));
                case CONTINUOUS -> Component.translatable("clickguard.hud.continuous",
                        clicker.preset.name, Component.translatable(clicker.preset.keybind.getName()));
                case COOLDOWN_AWARE -> Component.translatable("clickguard.hud.cooldownAware",
                        clicker.preset.name, Component.translatable(clicker.preset.keybind.getName()));
            };

            graphics.text(font, text, x, y, CommonColors.TEXT_GRAY);
            y += lineHeight;
        }

        if (clickers.isEmpty()) {
            graphics.text(font, Component.translatable("clickguard.hud.noPresetsEnabled.line1"), x, y, CommonColors.TEXT_GRAY);
            y += lineHeight;
            graphics.text(font, Component.translatable("clickguard.hud.noPresetsEnabled.line2", openPresetsScreen.getTranslatedKeyMessage()), x, y, CommonColors.TEXT_GRAY);
        }

        renderWarning(graphics);
    }

    public static long warningStartTime = -1; // -1 = warning not active
    private static final long WARNING_DURATION_MS = 4000;
    private static final long FADE_DURATION_MS = 800;


    public static void triggerWarning() {
        warningStartTime = Util.getMillis();
    }

    private static void renderWarning(GuiGraphicsExtractor graphics) {
        if (warningStartTime < 0) return;

        long elapsed = Util.getMillis() - warningStartTime;
        if (elapsed >= WARNING_DURATION_MS) {
            warningStartTime = -1; // Warning over
            return;
        }

        float alpha = 1.0F;
        long fadeStart = WARNING_DURATION_MS - FADE_DURATION_MS;
        if (elapsed > fadeStart) {
            long fadeElapsed = elapsed - fadeStart;
            alpha = 1.0F - (float) fadeElapsed / FADE_DURATION_MS;
        }

        int alphaInt = (int) (alpha * 255);
        int color = (alphaInt << 24) | 0xFCFC54;

        Font font = Minecraft.getInstance().font;
        Component text = Component.translatable("clickguard.hud.warning.manualKeypress");
        int width = font.width(text);

        int x = (graphics.guiWidth() - width) / 2;
        int y = graphics.guiHeight() / 3;

        graphics.text(font, text, x, y, color);
    }
}
