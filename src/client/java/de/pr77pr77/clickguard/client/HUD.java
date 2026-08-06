package de.pr77pr77.clickguard.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;

import static de.pr77pr77.clickguard.client.ClickGuardClient.*;

public class HUD {
    public static void renderClickerHud(GuiGraphicsExtractor graphics, DeltaTracker tickCounter) {
        if (!autoClickingEnabled){
            return;
        }

        Font font = Minecraft.getInstance().font;
        int x = 5;
        int y = 5;
        int lineHeight = 10;

        graphics.text(font, Component.translatable("clickguard.hud.title"), x, y, CommonColors.TEXT_GRAY);
        y += lineHeight;

        for (Clicker clicker : clickers) {
            Component text = switch(clicker.preset.clickingType) {
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

        if(clickers.isEmpty()){
            graphics.text(font, Component.translatable("clickguard.hud.noPresetsEnabled.line1"), x, y, CommonColors.TEXT_GRAY);
            y += lineHeight;
            graphics.text(font, Component.translatable("clickguard.hud.noPresetsEnabled.line2", openPresetsScreen.getTranslatedKeyMessage()), x, y, CommonColors.TEXT_GRAY);
        }
    }
}
