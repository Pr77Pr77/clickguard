package de.pr77pr77.clickguard.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import net.minecraft.Util;

import static de.pr77pr77.clickguard.client.ClickGuardClient.*;

public class HUD {
    public static void renderClickerHud(GuiGraphics graphics, DeltaTracker tickCounter) {
        if (!autoClickingEnabled && autoStoppedInfo == null) {
            return;
        }

        Font font = Minecraft.getInstance().font;
        int x = 5;
        int y = 5;
        int lineHeight = 10;

        if (autoStoppedInfo != null) {
            // Render auto stopped info:
            graphics.drawString(font, Component.translatable("clickguard.action.hudStopped.title"), x, y, CommonColors.SOFT_YELLOW);
            y += lineHeight;
            graphics.drawString(font, autoStoppedInfo.reasonMessage, x, y, CommonColors.SOFT_YELLOW);
            y += lineHeight;
            graphics.drawString(font, Component.translatable("clickguard.action.hudStopped.subtitle", enableClickingKey.getTranslatedKeyMessage()), x, y, TEXT_GRAY);
            return;
        }


        graphics.drawString(font, Component.translatable("clickguard.hud.title"), x, y, TEXT_GRAY);
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

            graphics.drawString(font, text, x, y, TEXT_GRAY);
            y += lineHeight;
        }

        if (clickers.isEmpty()) {
            graphics.drawString(font, Component.translatable("clickguard.hud.noPresetsEnabled.line1"), x, y, TEXT_GRAY);
            y += lineHeight;
            graphics.drawString(font, Component.translatable("clickguard.hud.noPresetsEnabled.line2", openPresetsScreen.getTranslatedKeyMessage()), x, y, TEXT_GRAY);
        }

        renderWarning(graphics);
    }

    public static long warningStartTime = -1; // -1 = warning not active
    private static final long WARNING_DURATION_MS = 4000;
    private static final long FADE_DURATION_MS = 800;


    public static void triggerWarning() {
        warningStartTime = Util.getMillis();
    }

    private static void renderWarning(GuiGraphics graphics) {
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

        graphics.drawString(font, text, x, y, color);
    }
}
