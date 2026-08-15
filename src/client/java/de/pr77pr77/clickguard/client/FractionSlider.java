package de.pr77pr77.clickguard.client;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class FractionSlider extends AbstractSliderButton {
    private final Consumer<Double> responder;
    private final String messageTranslationKey;

    public FractionSlider(int x, int y, int width, int height, String messageTranslationKey, final double initialValue, final Consumer<Double> responder) {
        super(x, y, width, height, Component.translatable(messageTranslationKey, String.format("%.2f", initialValue * 100)), initialValue);
        this.messageTranslationKey = messageTranslationKey;
        this.responder = responder;
        updateMessage();
    }

    @Override
    protected void updateMessage() {
        this.setMessage(Component.translatable(messageTranslationKey, String.format("%.2f", value * 100)));
    }

    @Override
    protected void applyValue() {
        if (value > 0.9999d) {
            this.setValue(0.9999d);
        }
        responder.accept(value);
    }
}
