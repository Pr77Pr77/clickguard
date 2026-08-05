package de.pr77pr77.clickguard.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class ChooseKeybindScreen extends Screen {
    private final Screen parent;

    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    private KeybindsList list;

    private final ConfigManager.ConfigData.Preset preset;

    protected ChooseKeybindScreen(Screen parent) {
        super(Component.translatable("clickguard.keybind.choose"));
        this.parent = parent;
        this.preset = null;
    }

    protected ChooseKeybindScreen(Screen parent, ConfigManager.ConfigData.Preset preset) {
        super(Component.translatable("clickguard.keybind.change"));
        this.parent = parent;
        this.preset = preset;
    }

    @Override
    protected void init() {
        layout.addTitleHeader(title, font);

        list = layout.addToContents(new KeybindsList(minecraft, width, layout, this));
        list.fillList();

        LinearLayout footerButtons = LinearLayout.horizontal().spacing(8);
        footerButtons.addChild(Button.builder(CommonComponents.GUI_CANCEL, _ -> onClose()).build());
        layout.addToFooter(footerButtons);

        layout.visitWidgets(this::addRenderableWidget);
        repositionElements();
    }

    @Override
    public void repositionElements() {
        layout.arrangeElements();
        if (list != null) {
            list.updateSize(width, layout);
        }
        assert list != null;
        for (KeybindsList.Entry entry : list.children()) {
            entry.init();
        }
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }

    public static class KeybindsList extends ContainerObjectSelectionList<KeybindsList.Entry> {
        private final ChooseKeybindScreen screen;

        public KeybindsList(Minecraft minecraft, int width, HeaderAndFooterLayout layout, ChooseKeybindScreen screen) {
            super(minecraft, width, layout.getContentHeight(), layout.getHeaderHeight(), 30);
            this.screen = screen;
        }

        public void fillList() {
            KeyMapping[] keyMappings = minecraft.options.keyMappings;
            for (KeyMapping keyMapping : keyMappings) {
                addEntry(new Entry(minecraft, keyMapping, keyMappingClicked -> {
                    if (screen.preset != null) {
                        screen.preset.keybind = keyMappingClicked;
                        minecraft.gui.setScreen(screen.parent);
                    } else {
                        minecraft.gui.setScreen(new EditPresetScreen(screen.parent, keyMappingClicked));
                    }
                }));
            }
        }

        @Override
        public int getRowWidth() {
            return Math.min(500, width - 40);
        }

        @Override
        protected int scrollBarX() {
            return this.width - 6;
        }

        public static class Entry extends ContainerObjectSelectionList.Entry<Entry> {
            private final Minecraft minecraft;
            private final KeyMapping keyMapping;
            private final Button selectButton;

            private Entry(Minecraft minecraft, KeyMapping keyMapping, java.util.function.Consumer<KeyMapping> onSelect) {
                this.minecraft = minecraft;
                this.keyMapping = keyMapping;
                this.selectButton = Button.builder(Component.translatable("clickguard.keybind.select"),
                        _ -> onSelect.accept(keyMapping)).build(); // Size and position are set in init()
            }

            void init() {
                int buttonWidth = 80;
                int buttonHeight = 20;
                selectButton.setPosition(getContentX() + getContentWidth() - buttonWidth - 8, getContentY() + (getContentHeight() - buttonHeight) / 2);
                selectButton.setSize(buttonWidth, buttonHeight);
            }

            @Override
            public void extractContent(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
                int textX = getContentX() + 8;
                int textY = getContentY() + (getContentHeight() - minecraft.font.lineHeight * 2 - 2) / 2;

                graphics.fill(getContentX(), getContentY(), getContentX() + getContentWidth(), getContentY() + getContentHeight(), 0x44000000);

                graphics.text(minecraft.font, Component.translatable(keyMapping.getName()), textX, textY, 0xFFFFFFFF, true);
                graphics.text(minecraft.font, keyMapping.getTranslatedKeyMessage(), textX, textY + minecraft.font.lineHeight + 2, 0xFFAAAAAA, true);
                selectButton.setY(getContentY() + (getContentHeight() - selectButton.getHeight()) / 2);
                selectButton.extractRenderState(graphics, mouseX, mouseY, a);
            }

            @Override
            public @NonNull List<? extends GuiEventListener> children() {
                return List.of(selectButton);
            }

            @Override
            public @NonNull List<? extends NarratableEntry> narratables() {
                return List.of(selectButton);
            }
        }
    }
}
