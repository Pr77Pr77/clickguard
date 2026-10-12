package de.pr77pr77.clickguard.client.screens;

import net.minecraft.client.Minecraft;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class ItemFilterMenu extends AbstractContainerMenu {
    List<ItemStack> items;
    SelectionContainer selectionContainer = new SelectionContainer();
    Slot lastSlot; // Empty Slot

    final int initialRows;

    protected ItemFilterMenu(List<ItemStack> items, Inventory inventory) {
        super(null, -1); // No container!
        this.items = items;
        initialRows = getRows();
        addStandardInventorySlots(inventory, 8, 18 + getRows() * 18 + 13);
        updateSlots();
    }

    @Override
    public @NonNull ItemStack quickMoveStack(@NonNull Player player, int slotIndex) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(@NonNull Player player) {
        return true;
    }

    int getRows() {
        return Math.min(items.size() / 9 + 1, 6);
    }

    void updateSlots() {
        if (getRows() != initialRows && Minecraft.getInstance().player != null && Minecraft.getInstance().screen instanceof ItemFilterSelectScreen oldScreen) {
            Minecraft.getInstance().setScreen(new ItemFilterSelectScreen(Minecraft.getInstance().player, items, oldScreen.parent));
        }
        slots.removeIf(slot -> (slot instanceof ItemFilterSlot));
        for (int y = 0; y < getRows(); ++y) {
            int slotsInRow = (y < (items.size() + 1) / 9) ? 9 : ((items.size() + 1) % 9);
            for (int x = 0; x < slotsInRow; ++x) {
                lastSlot = new ItemFilterSlot(selectionContainer, x + y * 9, 8 + x * 18, 18 + y * 18);
                this.addSlot(lastSlot);
            }
        }
    }

    @Override
    public void clicked(int slotIndex, int buttonNum, @NonNull ClickType containerInput, @NonNull Player player) {
        if (slotIndex < 0) {
            setCarried(ItemStack.EMPTY);
            return;
        }
        if (slots.get(slotIndex) instanceof ItemFilterSlot) {
            ItemStack carried = getCarried();
            if (!carried.isEmpty()) {
                slots.get(slotIndex).set(carried.copy()); // Copy, original stays carried
                setCarried(ItemStack.EMPTY);
                updateSlots();
                return;
            }
            if (slots.get(slotIndex).hasItem()) {
                if (buttonNum == 0 && (!slots.get(slotIndex).getItem().getComponentsPatch().split().added().isEmpty() ||
                        slots.get(slotIndex).getItem().getMaxStackSize() > 1) &&
                        Minecraft.getInstance().screen instanceof ItemFilterSelectScreen screen) {
                    Minecraft.getInstance().setScreen(new ItemFilterEditInfoScreen(screen, slots.get(slotIndex).getItem()));
                } else if (buttonNum == 1) {
                    items.remove(slots.get(slotIndex).getItem());
                    updateSlots();
                }
                return;
            }
        }
        // Inventory slots:
        if (containerInput == ClickType.QUICK_MOVE && slots.get(slotIndex).hasItem()) {
            lastSlot.set(slots.get(slotIndex).getItem().copy()); // Copy Item to empty slot
            updateSlots();
        } else if (containerInput == ClickType.PICKUP) {
            ItemStack toCopy = slots.get(slotIndex).getItem();
            int amount = buttonNum == 0 ? toCopy.getCount() : (toCopy.getCount() + 1) / 2;
            ItemStack copy = toCopy.copyWithCount(amount);
            setCarried(copy);
            slots.get(slotIndex).onTake(player, copy);
        }
    }

    public class SelectionContainer implements Container, StackedContentsCompatible {
        @Override
        public int getContainerSize() {
            return items.size() + 1; // +1 for empty slot
        }

        @Override
        public boolean isEmpty() {
            return false; // Always at least one slot
        }

        @Override
        public @NonNull ItemStack getItem(int slot) {
            return (slot >= 0 && slot < items.size()) ? items.get(slot) : ItemStack.EMPTY;
        }

        @Override
        public @NonNull ItemStack removeItem(int slot, int count) {
            return ContainerHelper.removeItem(items, slot, count);
        }

        @Override
        public @NonNull ItemStack removeItemNoUpdate(int slot) {
            ItemStack itemStack = items.get(slot);
            if (!itemStack.isEmpty()) {
                items.set(slot, ItemStack.EMPTY);
            }
            return itemStack;
        }

        @Override
        public void setItem(int slot, @NonNull ItemStack itemStack) {
            if (slot < items.size()) {
                items.set(slot, itemStack);
            } else if (items.size() <= 54) {
                items.add(itemStack);
            }
            itemStack.limitSize(getMaxStackSize(itemStack));
        }

        @Override
        public void setChanged() { // Not needed
        }

        @Override
        public boolean stillValid(@NonNull Player player) {
            return true;
        }

        @Override
        public void clearContent() {
            items.clear();
        }

        @Override
        public void fillStackedContents(@NonNull StackedItemContents contents) { // Not needed, as no recipes need to be evaluated.
        }
    }

    public static class ItemFilterSlot extends Slot {
        public ItemFilterSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }
    }
}
