package de.pr77pr77.clickguard.client.screens;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemFilterEditInfoScreen extends Screen {
    ItemFilterSelectScreen parent;
    ItemStack itemStack;

    protected ItemFilterEditInfoScreen(ItemFilterSelectScreen parent, ItemStack itemStack) {
        super(Component.translatable("clickguard.filter.item.editInformation"));
        this.parent = parent;
        this.itemStack = itemStack;
    }

    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    private ItemComponentList list;

    @Override
    protected void init() {
        layout.addTitleHeader(title, font);

        list = layout.addToContents(new ItemComponentList(minecraft, width, layout, itemStack));
        list.fillList();

        LinearLayout footerButtons = LinearLayout.horizontal().spacing(8);
        footerButtons.addChild(Button.builder(CommonComponents.GUI_DONE, button -> onClose()).build());
        layout.addToFooter(footerButtons);

        layout.visitWidgets(this::addRenderableWidget);
        repositionElements();
    }

    @Override
    public void repositionElements() {
        layout.arrangeElements();
        if (list != null) {
            list.updateSize(width, layout);
            for (ItemComponentList.ComponentEntry entry : list.children()) {
                entry.init();
            }
        }
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    public class ItemComponentList extends ContainerObjectSelectionList<ItemComponentList.ComponentEntry> {

        private final ItemStack stack;

        public ItemComponentList(Minecraft minecraft, int width, HeaderAndFooterLayout layout, ItemStack stack) {
            super(minecraft, width, layout.getContentHeight(), layout.getHeaderHeight(), 30);
            this.stack = stack;
            fillList();
        }

        private void fillList() {
            clearEntries();
            if (stack.getMaxStackSize() > 1) {
                addEntry(new CountEntry());
            }
            for (TypedDataComponent<?> component : stack.getComponentsPatch().split().added()) {
                addEntry(new DeletableEntry(component));
            }
        }

        @Override
        protected int addEntry(@NonNull ComponentEntry entry) {
            int returnValue = super.addEntry(entry);
            entry.init();
            return returnValue;
        }

        @Override
        public int getRowWidth() {
            return Math.min(500, width - 40);
        }

        public abstract static class ComponentEntry extends ContainerObjectSelectionList.Entry<ComponentEntry> {
            protected final List<AbstractWidget> widgets = new ArrayList<>();

            abstract void init();

            @Override
            public @NonNull List<? extends GuiEventListener> children() {
                return widgets;
            }

            @Override
            public @NonNull List<? extends NarratableEntry> narratables() {
                return widgets;
            }

            @Override
            public void renderContent(GuiGraphics graphics, int mouseX, int mouseY, boolean hovered, float a) {
                graphics.fill(getContentX(), getContentY(), getContentX() + getContentWidth(), getContentY() + getContentHeight(), 0x44000000);
                for (AbstractWidget widget : widgets) {
                    widget.setY(getContentYMiddle() - widget.getHeight() / 2);
                    widget.render(graphics, mouseX, mouseY, a);
                }
            }
        }

        public class DeletableEntry extends ComponentEntry {
            private final TypedDataComponent<?> component;
            final MultiLineTextWidget text;
            final Button ignoreButton;

            public DeletableEntry(TypedDataComponent<?> component) {
                this.component = component;

                text = new MultiLineTextWidget(getDisplayText(), font);
                text.setMaxRows(1);
                text.setCentered(false);
                widgets.add(text);

                ignoreButton = Button.builder(Component.translatable("clickguard.filter.item.editInformation.ignore"), button -> onDelete())
                        .bounds(0, 0, 70, 20)
                        .build();
                widgets.add(ignoreButton);
            }

            @Override
            void init() {
                text.setX(getContentX() + 5);
                text.setMaxWidth(getContentWidth() - 10 - 70);

                ignoreButton.setX(getContentRight() - 70 - 5);
            }

            private void onDelete() {
                resetToDefault(stack, component.type());
                fillList();
            }

            public Component getDisplayText() {
                MutableComponent displayText;
                if (COMPONENT_NAMES.containsKey(component.type())) {
                    displayText = COMPONENT_NAMES.get(component.type()).copy();
                } else {
                    displayText = Component.literal(component.type().toString());
                }
                if (component.value() instanceof Number || component.value() instanceof String || component.value() instanceof Character) {
                    displayText.append(Component.literal(": " + component.value()));
                } else if (component.value() instanceof Boolean boolValue) {
                    displayText.append(Component.literal(": " + (boolValue ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF)));
                }
                return displayText;
            }

            private static <T> void resetToDefault(ItemStack stack, DataComponentType<T> type) {
                stack.set(type, stack.getPrototype().get(type));
            }

            public static final Map<DataComponentType<?>, Component> COMPONENT_NAMES = new HashMap<>();

            static {
                COMPONENT_NAMES.put(DataComponents.ITEM_NAME, Component.translatable("clickguard.filter.item.editInformation.component.item_name"));
                COMPONENT_NAMES.put(DataComponents.CUSTOM_NAME, Component.translatable("clickguard.filter.item.editInformation.component.custom_name"));
                COMPONENT_NAMES.put(DataComponents.LORE, Component.translatable("clickguard.filter.item.editInformation.component.lore"));

                COMPONENT_NAMES.put(DataComponents.ENCHANTMENTS, Component.translatable("clickguard.filter.item.editInformation.component.enchantments"));
                COMPONENT_NAMES.put(DataComponents.STORED_ENCHANTMENTS, Component.translatable("clickguard.filter.item.editInformation.component.stored_enchantments"));
                COMPONENT_NAMES.put(DataComponents.REPAIR_COST, Component.translatable("clickguard.filter.item.editInformation.component.repair_cost"));

                COMPONENT_NAMES.put(DataComponents.DAMAGE, Component.translatable("clickguard.filter.item.editInformation.component.damage"));
                COMPONENT_NAMES.put(DataComponents.UNBREAKABLE, Component.translatable("clickguard.filter.item.editInformation.component.unbreakable"));

                COMPONENT_NAMES.put(DataComponents.RARITY, Component.translatable("clickguard.filter.item.editInformation.component.rarity"));
                COMPONENT_NAMES.put(DataComponents.DYED_COLOR, Component.translatable("clickguard.filter.item.editInformation.component.dyed_color"));
                COMPONENT_NAMES.put(DataComponents.POTION_CONTENTS, Component.translatable("clickguard.filter.item.editInformation.component.potion_contents"));
                COMPONENT_NAMES.put(DataComponents.OMINOUS_BOTTLE_AMPLIFIER, Component.translatable("clickguard.filter.item.editInformation.component.ominous_bottle_amplifier"));
                COMPONENT_NAMES.put(DataComponents.ITEM_MODEL, Component.translatable("clickguard.filter.item.editInformation.component.item_model"));
                COMPONENT_NAMES.put(DataComponents.TOOLTIP_DISPLAY, Component.translatable("clickguard.filter.item.editInformation.component.tooltip_display"));
                COMPONENT_NAMES.put(DataComponents.BANNER_PATTERNS, Component.translatable("clickguard.filter.item.editInformation.component.banner_patterns"));
                COMPONENT_NAMES.put(DataComponents.TRIM, Component.translatable("clickguard.filter.item.editInformation.component.trim"));

                COMPONENT_NAMES.put(DataComponents.CONTAINER, Component.translatable("clickguard.filter.item.editInformation.component.container"));


                COMPONENT_NAMES.put(DataComponents.CUSTOM_DATA, Component.translatable("clickguard.filter.item.editInformation.component.custom_data"));
            }
        }

        public class CountEntry extends ComponentEntry {
            final EditBox countBox;
            final MultiLineTextWidget text;

            CountEntry() {
                text = new MultiLineTextWidget(Component.translatable("clickguard.filter.item.editInformation.count"), font);
                text.setMaxRows(1);
                text.setCentered(false);
                widgets.add(text);

                countBox = new EditBox(Minecraft.getInstance().font, 0, 0, 70, 20,
                        Component.translatable("clickguard.filter.item.editInformation.count"));
                countBox.setValue(String.valueOf(stack.getCount()));
                countBox.setResponder(this::onCountChanged);
                widgets.add(countBox);
            }

            @Override
            void init() {
                text.setX(getContentX() + 5);
                text.setMaxWidth(getContentWidth() - 10 - 70);
                countBox.setX(getContentRight() - 70 - 5);
            }

            private void onCountChanged(String newValue) {
                try {
                    int count = Integer.parseInt(newValue.trim());
                    count = Math.clamp(count, 1, stack.getMaxStackSize());
                    stack.setCount(count);
                } catch (NumberFormatException ignored) {
                    // No overriding EditBox value to prevent annoyance.
                }
            }
        }
    }
}
