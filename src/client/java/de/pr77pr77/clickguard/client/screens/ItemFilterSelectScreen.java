package de.pr77pr77.clickguard.client.screens;

import com.mojang.blaze3d.platform.NativeImage;
import de.pr77pr77.clickguard.client.ClickGuardClient;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

public class ItemFilterSelectScreen extends AbstractContainerScreen<ItemFilterMenu> {
    private static final Identifier INVENTORY_BACKGROUND = Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");

    Screen parent;
    int backgroundColor = getPixelColor(INVENTORY_BACKGROUND, 169, 18);

    public ItemFilterSelectScreen(Player player, List<ItemStack> items, Screen parent) {
        super(new ItemFilterMenu(items, player.getInventory()), player.getInventory(), Component.translatable("clickguard.filter.item.selection"),
                176, Math.min(items.size() / 9 + 1, 6) * 18 + 17 + 96);
        this.parent = parent;

    }

    @Override
    public void onClose() {
        ClickGuardClient.configManager.save();
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    @Override
    protected void slotClicked(final @Nullable Slot slot, int slotId, final int buttonNum, final @NonNull ContainerInput containerInput) {
        if (minecraft.player != null) {
            menu.clicked(slotId, buttonNum, containerInput, minecraft.player);
        }
    }

    @Override
    public void extractBackground(final @NonNull GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int x = (width - imageWidth) / 2;
        int yTop = (height - imageHeight) / 2;
        graphics.blit(RenderPipelines.GUI_TEXTURED, INVENTORY_BACKGROUND, x, yTop, 0.0F, 0.0F, imageWidth, menu.getRows() * 18 + 17, 256, 256);
        graphics.fill(RenderPipelines.GUI, x + menu.lastSlot.x + 17, yTop + (menu.getRows() - 1) * 18 + 17,
                x + 169, yTop + menu.getRows() * 18 + 17, backgroundColor);
        graphics.blit(RenderPipelines.GUI_TEXTURED, INVENTORY_BACKGROUND, x, yTop + menu.getRows() * 18 + 17, 0.0F, 126.0F, imageWidth, 96, 256, 256);
    }

    @Override
    protected @NonNull List<Component> getTooltipFromContainerItem(@NonNull ItemStack itemStack) {
        if (hoveredSlot == null || !(hoveredSlot instanceof ItemFilterMenu.ItemFilterSlot)) {
            return super.getTooltipFromContainerItem(itemStack);
        }

        List<Component> tooltip = super.getTooltipFromContainerItem(itemStack);
        tooltip.add((itemStack.getComponentsPatch().split().added().isEmpty() && itemStack.getMaxStackSize() <= 1) ?
                Component.translatable("clickguard.filter.item.clickDelete") :
                Component.translatable("clickguard.filter.item.clickActions"));
        return tooltip;
    }

    @Override
    protected void extractTooltip(final @NonNull GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
        if (hoveredSlot != null && !hoveredSlot.hasItem() && hoveredSlot instanceof ItemFilterMenu.ItemFilterSlot) {
            graphics.setTooltipForNextFrame(this.font, Component.translatable("clickguard.filter.item.putHere"), mouseX, mouseY);
        } else {
            super.extractTooltip(graphics, mouseX, mouseY);
        }
    }

    public int getPixelColor(Identifier textureLocation, int x, int y) {
        Optional<Resource> texture = minecraft.getResourceManager().getResource(textureLocation);

        if (texture.isPresent()) {
            try (InputStream stream = texture.get().open();
                 NativeImage image = NativeImage.read(stream)) {

                return image.getPixel(x, y);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return 0x00000000; // Fallback: transparent
    }
}
