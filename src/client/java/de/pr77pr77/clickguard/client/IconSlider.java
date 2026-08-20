package de.pr77pr77.clickguard.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

import static de.pr77pr77.clickguard.ClickGuard.id;

public class IconSlider extends AbstractSliderButton {
    static final int SPRITE_SIZE = 9;
    static final int HANDLE_WIDTH = 9;
    static final int HANDLE_HEIGHT = 8;
    static final int HANDLE_HALF_WIDTH = 4; // Border width left and right
    static final int HEIGHT = 25;

    static final ResourceLocation HANDLE_SPRITE = id("widget/arrow_slider_handle");
    static final ResourceLocation HANDLE_HIGHLIGHTED_SPRITE = id("widget/arrow_slider_handle_highlighted");

    private final IconSprites sprites;
    public final int maxShownPoints;
    public final int maxRealPoints;
    public int selectedPoints;
    private final Consumer<Integer> responder;
    private final String messageTranslationKey;

    public IconSlider(int x, int y, final IconSprites sprites, final int maxShownPoints, final int maxRealPoints, String messageTranslationKey, final int initialPoints, final Consumer<Integer> responder) {
        super(x, y, (HANDLE_WIDTH - 1) * maxShownPoints / 2 + HANDLE_HALF_WIDTH * 2 + 1, HEIGHT, Component.translatable(messageTranslationKey, initialPoints), initialPoints / (double) maxShownPoints);
        this.sprites = sprites;
        this.maxShownPoints = maxShownPoints;
        this.maxRealPoints = maxRealPoints;
        this.messageTranslationKey = messageTranslationKey;
        this.responder = responder;
        selectedPoints = initialPoints;
        updateMessage();
    }

    public IconSlider(int x, int y, final IconSprites sprites, final int maxShownPoints, String messageTranslationKey, final int initialPoints, final Consumer<Integer> responder) {
        this(x, y, sprites, maxShownPoints, maxShownPoints - 1, messageTranslationKey, initialPoints, responder);
    }

    @Override
    protected void updateMessage() {
        this.setMessage(Component.translatable(messageTranslationKey, selectedPoints));
        this.setTooltip(Tooltip.create(Component.translatable(messageTranslationKey, selectedPoints)));
    }

    @Override
    protected void applyValue() {
        selectedPoints = Math.toIntExact(Math.min(Math.round(value * maxShownPoints), maxRealPoints));
        responder.accept(selectedPoints);
    }

    @Override
    public void renderWidget(final GuiGraphics graphics, final int mouseX, final int mouseY, final float a) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, getSprite(), getX(), getY(), getWidth(), getHeight(), ARGB.white(alpha));
        for (int iconIndex = 0; iconIndex < this.maxShownPoints / 2; ++iconIndex) {
            // Always render the empty icon below:
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprites.empty,
                    getX() + HANDLE_HALF_WIDTH + iconIndex * (SPRITE_SIZE - 1),
                    getY() + (getHeight() - SPRITE_SIZE) / 2,
                    SPRITE_SIZE, SPRITE_SIZE);

            if (iconIndex < selectedPoints / 2) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprites.full,
                        getX() + HANDLE_HALF_WIDTH + iconIndex * (SPRITE_SIZE - 1),
                        getY() + (getHeight() - SPRITE_SIZE) / 2,
                        SPRITE_SIZE, SPRITE_SIZE);
            } else if (iconIndex < (selectedPoints / 2 + selectedPoints % 2)) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprites.half,
                        getX() + HANDLE_HALF_WIDTH + iconIndex * (SPRITE_SIZE - 1),
                        getY() + (getHeight() - SPRITE_SIZE) / 2,
                        SPRITE_SIZE, SPRITE_SIZE);
            }
        }
        extractHandle(graphics);
    }

    private void extractHandle(final GuiGraphics graphics) {
        int spriteX = this.getX() + (int) (this.value * (double) (this.width - HANDLE_WIDTH));

        // Top:
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED,
                !this.isActive() || !this.isHovered && !this.canChangeValue ? HANDLE_SPRITE : HANDLE_HIGHLIGHTED_SPRITE,
                spriteX, this.getY(), HANDLE_WIDTH, HANDLE_HEIGHT, ARGB.white(this.alpha));

        int spriteY = this.getBottom() - HANDLE_HEIGHT;
        float centerX = spriteX + HANDLE_WIDTH / 2f;
        float centerY = spriteY + HANDLE_HEIGHT / 2f;

        // Bottom:
        graphics.pose().pushMatrix();
        graphics.pose().translate(centerX, centerY);
        graphics.pose().scale(-1f, -1f);
        graphics.pose().translate(-centerX, -centerY);

        graphics.blitSprite(RenderPipelines.GUI_TEXTURED,
                !this.isActive() || !this.isHovered && !this.canChangeValue ? HANDLE_SPRITE : HANDLE_HIGHLIGHTED_SPRITE,
                spriteX, spriteY, HANDLE_WIDTH, HANDLE_HEIGHT, ARGB.white(this.alpha));

        graphics.pose().popMatrix();
    }

    @Override
    public void onRelease(final @NotNull MouseButtonEvent event) {
        // Set value to exact value to let the handle snap into place:
        value = selectedPoints / (double) maxShownPoints;
        super.onRelease(event);
    }

    @Override
    public boolean keyPressed(final KeyEvent event) {
        if (event.isSelection()) {
            this.canChangeValue = !this.canChangeValue;
            return true;
        } else if (this.canChangeValue) {
            if (event.isLeft()) {
                setValue((selectedPoints - 1) / (double) maxShownPoints);
                return true;
            } else if (event.isRight()) {
                if (selectedPoints < maxRealPoints) {
                    setValue((selectedPoints + 1) / (double) maxShownPoints);
                }
                return true; // Consume click even when can't go further right
            }
        }
        return false;
    }

    public static class IconSprites {
        private final ResourceLocation full;
        private final ResourceLocation empty;

        private final ResourceLocation half;

        public IconSprites(ResourceLocation full, ResourceLocation empty, ResourceLocation half) {
            this.full = full;
            this.empty = empty;
            this.half = half;
        }
    }
}
