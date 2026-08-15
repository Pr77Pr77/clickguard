package de.pr77pr77.clickguard.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.CommonColors;
import org.jspecify.annotations.NonNull;

import static de.pr77pr77.clickguard.ClickGuard.id;
import static de.pr77pr77.clickguard.client.ClickGuardClient.formatDuration;

public class AutoDisconnectScreen extends Screen {
    private int hoverTriggerCount = -1;
    private boolean wasHoveredLastFrame = false;
    private float titleOffsetY = 0f;
    private static final float TARGET_OFFSET = 30f;

    private static final int TEXTS_COUNT = 10;

    private static final int TITLE_WIDTH = 256;
    private static final int TITLE_HEIGHT = 46;
    private int titleBaseY;

    private MultiLineTextWidget textWidget;
    private Button toTitleScreenButton;

    private final ClickGuardClient.AutoDisconnectInfo disconnectInfo;

    public AutoDisconnectScreen(ClickGuardClient.AutoDisconnectInfo disconnectInfo) {
        super(Component.translatable("clickguard.autoDisconnectScreen"));
        this.disconnectInfo = disconnectInfo;
    }

    @Override
    public void init() {
        super.init();
        MutableComponent actionReasonComponent;
        if (disconnectInfo.causingPreset.playerDamaged == disconnectInfo.causingAction) {
            actionReasonComponent = Component.translatable("clickguard.autoDisconnectScreen.reason.damage");
        } else if (disconnectInfo.causingAction instanceof ConfigManager.ConfigData.SliderAction sliderAction &&
                disconnectInfo.causingPreset.healthActions.contains(sliderAction)) {
            actionReasonComponent = Component.translatable("clickguard.autoDisconnectScreen.reason.health", sliderAction.points);
        } else if (disconnectInfo.causingAction instanceof ConfigManager.ConfigData.SliderAction sliderAction &&
                disconnectInfo.causingPreset.hungerActions.contains(sliderAction)) {
            actionReasonComponent = Component.translatable("clickguard.autoDisconnectScreen.reason.hunger", sliderAction.points);
        } else if (disconnectInfo.causingAction instanceof ConfigManager.ConfigData.FractionAction fractionAction &&
                disconnectInfo.causingPreset.durabilityActions.contains(fractionAction)) {
            actionReasonComponent = Component.translatable("clickguard.autoDisconnectScreen.reason.durability", String.format("%.2f", fractionAction.fraction * 100));
        } else if (disconnectInfo.causingAction instanceof ConfigManager.ConfigData.TimeAction timeAction &&
                disconnectInfo.causingPreset.waitTimeActions.contains(timeAction)) {
            actionReasonComponent = Component.translatable("clickguard.autoDisconnectScreen.reason.waitTime", formatDuration(timeAction.timeMS));
        } else {
            actionReasonComponent = Component.translatable("clickguard.autoDisconnectScreen.reason.unknown");
        }
        actionReasonComponent.withStyle(ChatFormatting.BOLD);
        Component currentHealthAndHungerComponent = Component.translatable("clickguard.autoDisconnectScreen.currentHealthAndHunger",
                disconnectInfo.health, disconnectInfo.hunger).withStyle(ChatFormatting.BOLD);
        textWidget = new MultiLineTextWidget(20, titleBaseY + TITLE_HEIGHT + 20, Component.translatable("clickguard.autoDisconnectScreen.informationText",
                disconnectInfo.causingPreset.name, actionReasonComponent, currentHealthAndHungerComponent), font);
        textWidget.setCentered(true);
        addRenderableWidget(textWidget);

        toTitleScreenButton = Button.builder(Component.translatable("clickguard.autoDisconnectScreen.toTitleScreen"),
                        _ -> Minecraft.getInstance().gui.setScreen(new TitleScreen()))
                .bounds((width - 150) / 2, textWidget.getBottom() + 20, 150, 20).build();
        addRenderableWidget(toTitleScreenButton);

        repositionElements();
    }

    @Override
    public void repositionElements() {
        textWidget.setMaxWidth(width - 40);

        titleBaseY = (height - (TITLE_HEIGHT + 20 + textWidget.getHeight() + 20 + toTitleScreenButton.getHeight())) / 2;

        textWidget.setY(titleBaseY + TITLE_HEIGHT + 20);

        toTitleScreenButton.setPosition((width - 150) / 2, textWidget.getBottom() + 20);
    }

    @Override
    public void extractRenderState(final @NonNull GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);

        boolean isHovered = isMouseOverTitle(mouseX, mouseY);

        float target = isHovered ? TARGET_OFFSET : 0f;
        titleOffsetY += (target - titleOffsetY) * 0.15f; // simple lerp/easing

        int titleX = (width - TITLE_WIDTH) / 2;
        int titleY = titleBaseY - (int) titleOffsetY;

        if (titleOffsetY > TARGET_OFFSET * 0.20f) {
            graphics.centeredText(font, Component.translatable("clickguard.autoDisconnectScreen.hiddenText." + (hoverTriggerCount + 1)), width / 2, titleBaseY + TITLE_HEIGHT - 20, CommonColors.TEXT_GRAY);
        }

        graphics.blit(RenderPipelines.GUI_TEXTURED, id("textures/gui/title/clickguard.png"), titleX, titleY, 0, 0, TITLE_WIDTH, TITLE_HEIGHT, TITLE_WIDTH, TITLE_HEIGHT);

        if (isHovered && titleOffsetY < TARGET_OFFSET * 0.20f && !wasHoveredLastFrame) {
            hoverTriggerCount = (hoverTriggerCount + 1) % TEXTS_COUNT; // Looping
        }
        wasHoveredLastFrame = isHovered;
    }

    private boolean isMouseOverTitle(int mouseX, int mouseY) {
        int titleX = (width - TITLE_WIDTH) / 2;
        int titleY = titleBaseY - (int) titleOffsetY;
        return mouseX >= titleX && mouseX <= titleX + TITLE_WIDTH
                && mouseY >= titleY && mouseY <= titleBaseY + TITLE_HEIGHT; // Box is getting larger when the title is lifted
    }
}
