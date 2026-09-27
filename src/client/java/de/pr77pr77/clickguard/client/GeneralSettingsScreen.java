package de.pr77pr77.clickguard.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

import java.util.List;

import static de.pr77pr77.clickguard.client.ClickGuardClient.*;

public class GeneralSettingsScreen extends Screen {
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    Screen parent;
    SettingsList list;

    public GeneralSettingsScreen(Screen parent) {
        this.parent = parent;
        super(Component.translatable("clickguard.generalSettings"));
    }

    @Override
    protected void init() {
        layout.addTitleHeader(title, font);

        list = layout.addToContents(new SettingsList(minecraft, width, layout));
        list.fillList();

        LinearLayout footerButtons = LinearLayout.horizontal().spacing(8);
        footerButtons.addChild(Button.builder(CommonComponents.GUI_DONE,
                _ -> onClose()).build());
        layout.addToFooter(footerButtons);

        layout.visitWidgets(this::addRenderableWidget);
        repositionElements();
    }

    @Override
    public void repositionElements() {
        layout.arrangeElements();
        if (list != null) list.updateSize(width, layout);
        assert list != null;
        for (SettingsList.Entry entry : list.children()) {
            entry.init();
        }
    }

    @Override
    public void added() {
        if (list != null) {
            list.clearEntries();
            list.fillList();
        }
    }

    @Override
    public void onClose() {
        if (parent != null) {
            minecraft.gui.setScreen(parent);
        } else {
            super.onClose();
        }
    }

    public static class SettingsList extends ContainerObjectSelectionList<SettingsList.Entry> {
        public SettingsList(Minecraft minecraft, int width, HeaderAndFooterLayout layout) {
            super(minecraft, width, layout.getContentHeight(), layout.getHeaderHeight(), 24);
        }

        public void fillList() {
            OnOffButtonEntry allowClickingEntry = new OnOffButtonEntry(Component.translatable("clickguard.generalSettings.allowClickingWhenEnabled"),
                    (_, value) -> {
                        configManager.data.allowClickingWhenEnabled = value;
                        ClickGuardClient.configManager.save();
                    }, configManager.data.allowClickingWhenEnabled);
            addEntry(allowClickingEntry, 24);
            allowClickingEntry.init();

            OnOffButtonEntry keepEnabledAfterDisconnect = new OnOffButtonEntry(Component.translatable("clickguard.generalSettings.keepEnabledAfterDisconnect"),
                    (_, value) -> {
                        configManager.data.keepEnabledAfterDisconnect = value;
                        ClickGuardClient.configManager.save();
                    }, configManager.data.keepEnabledAfterDisconnect);
            addEntry(keepEnabledAfterDisconnect, 24);
            keepEnabledAfterDisconnect.init();
        }

        @Override
        public int getRowWidth() {
            return Math.min(400, width - 50);
        }

        @Override
        protected int scrollBarX() {
            return this.width - 6;
        }

        public abstract static class Entry
                extends ContainerObjectSelectionList.Entry<Entry> {
            abstract void init(); // Initializer after adding, getContentWidth and positions available.
        }

        public static class OnOffButtonEntry extends Entry {
            private final CycleButton<Boolean> button;

            private OnOffButtonEntry(Component name, CycleButton.OnValueChange<Boolean> onValueChange, Boolean defaultValue) {
                button = CycleButton.builder((option) -> option ?
                                        Component.translatable("clickguard.on").withColor(0x54FC54) : Component.translatable("clickguard.off").withColor(0xFC5454),
                                defaultValue)
                        .withValues(List.of(Boolean.TRUE, Boolean.FALSE))
                        .create(0, 0, 0, 20, name, onValueChange); // Position and size set in init
            }

            @Override
            void init() {
                button.setPosition(getContentX(), getContentY() + (getContentHeight() - 20) / 2);
                button.setSize(getContentWidth(), 20);
            }

            @Override
            public void extractContent(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
                button.setY(getContentY());
                button.extractRenderState(graphics, mouseX, mouseY, a);
            }

            @Override
            public @NonNull List<? extends GuiEventListener> children() {
                return List.of(button);
            }

            @Override
            public @NonNull List<? extends NarratableEntry> narratables() {
                return List.of(button);
            }
        }
    }
}
