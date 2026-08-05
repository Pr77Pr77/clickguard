package de.pr77pr77.clickguard.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

import java.util.List;

import static de.pr77pr77.clickguard.client.ClickGuardClient.autoClickingEnabled;
import static de.pr77pr77.clickguard.client.ClickGuardClient.toggleAutoClickingEnabled;

public class PresetsScreen extends Screen {
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    PresetsList list;

    Button startStopButton;

    public PresetsScreen() {
        super(Component.translatable("clickguard.presets.title"));
    }

    @Override
    protected void init() {
        layout.addTitleHeader(title, font);

        list = layout.addToContents(new PresetsList(minecraft, width, layout));
        list.fillList();

        LinearLayout footerButtons = LinearLayout.horizontal().spacing(8);
        footerButtons.addChild(Button.builder(CommonComponents.GUI_DONE,
                _ -> onClose()).build());
        startStopButton = Button.builder(getStartStopButtonComponent(),
                _ -> toggleAutoClickingEnabled()).build();
        footerButtons.addChild(startStopButton);
        layout.addToFooter(footerButtons);

        layout.visitWidgets(this::addRenderableWidget);
        repositionElements();
    }

    @Override
    public void repositionElements() {
        layout.arrangeElements();
        if (list != null) list.updateSize(width, layout);
        assert list != null;
        for (PresetsList.Entry entry : list.children()) {
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

    static Component getStartStopButtonComponent() {
        return autoClickingEnabled ? Component.translatable("clickguard.stop") : Component.translatable("clickguard.start");
    }

    public static class PresetsList extends ContainerObjectSelectionList<PresetsList.Entry> {
        public PresetsList(Minecraft minecraft, int width, HeaderAndFooterLayout layout) {
            super(minecraft, width, layout.getContentHeight(), layout.getHeaderHeight(), 46);
        }

        public void fillList() {
            for (ConfigManager.ConfigData.Preset preset : ClickGuardClient.configManager.data.presets) {
                addPresetEntry(preset);
            }
            addCreateButon();
        }

        public void addCreateButon() {
            addEntry(new CreateButtonEntry(), 30);
        }

        public void addPresetEntry(ConfigManager.ConfigData.Preset preset) {
            addEntry(new PresetEnty(minecraft, preset));
        }

        public void updateEnableButtons() {
            for (Entry entry : children()) {
                if (entry instanceof PresetEnty presetEntry) {
                    presetEntry.enableButton.setValue(presetEntry.enableButton.getValue());
                }
            }
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

        public static class CreateButtonEntry extends Entry {
            private final Button button;

            private CreateButtonEntry() {
                button = Button.builder(Component.translatable("clickguard.presets.new"), _ -> {
                    Minecraft minecraft = Minecraft.getInstance();
                    minecraft.gui.setScreen(new ChooseKeybindScreen(minecraft.gui.screen()));
                }).build();
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

        public static class PresetEnty extends Entry {
            private final FocusableTextWidget name;
            private final FocusableTextWidget keybindAndBriefOptions;
            private final CycleButton<Boolean> enableButton;
            private final Button editButton;
            private final Button deleteButton;

            public PresetEnty(Minecraft minecraft, ConfigManager.ConfigData.Preset preset) {
                name = FocusableTextWidget.builder(Component.literal(preset.name), minecraft.font)
                        .backgroundFill(FocusableTextWidget.BackgroundFill.ON_FOCUS).alwaysShowBorder(false).build();
                name.setMaxRows(1);
                name.setCentered(false);
                keybindAndBriefOptions = FocusableTextWidget.builder(Component.translatable(preset.keybind.getName()), minecraft.font)
                        .backgroundFill(FocusableTextWidget.BackgroundFill.ON_FOCUS).alwaysShowBorder(false).build();
                keybindAndBriefOptions.setMaxRows(1);
                keybindAndBriefOptions.setCentered(false);

                Screen parentScreen = minecraft.gui.screen();

                enableButton = CycleButton.builder(PresetEnty::getColoredOnOffComponent,
                                preset.enabled)
                        .withValues(List.of(Boolean.TRUE, Boolean.FALSE))
                        .displayOnlyValue()
                        .create(0, 0, 0, 20, Component.translatable("manageServer.resourcePack.enabled"), (_, value) -> {
                            preset.enabled = value;
                            ClickGuardClient.configManager.save();
                        }); // Position and size set in init
                editButton = Button.builder(Component.translatable("selectServer.edit"),
                        _ -> minecraft.gui.setScreen(new EditPresetScreen(parentScreen, preset))).build();
                deleteButton = Button.builder(Component.translatable("selectServer.delete"),
                        _ -> minecraft.gui.setScreen(new ConfirmScreen(answer -> {
                            if (answer) {
                                ClickGuardClient.configManager.data.presets.remove(preset);
                                ClickGuardClient.configManager.save();
                            }
                            minecraft.gui.setScreen(parentScreen);
                        }, Component.translatable("clickguard.presets.delete.question"), Component.translatable("selectServer.deleteWarning", preset.name),
                                Component.translatable("selectWorld.deleteButton"), CommonComponents.GUI_CANCEL))).build();
            }

            public static Component getColoredOnOffComponent(Boolean option) {
                if (option) {
                    return autoClickingEnabled ? Component.translatable("options.on").withColor(0x54FC54) : Component.translatable("options.on").withColor(0xFCFC54);
                } else {
                    return Component.translatable("options.off").withColor(0xFC5454);
                }
            }

            @Override
            void init() {
                deleteButton.setRectangle(70, 20, getContentRight() - 4 - 70, getContentYMiddle() - 10);
                editButton.setRectangle(70, 20, deleteButton.getX() - 4 - 70, getContentYMiddle() - 10);
                enableButton.setRectangle(30, 20, editButton.getX() - 4 - 30, getContentYMiddle() - 10);

                name.setPosition(getContentX() + 4, getContentY() + 4);
                name.setMaxWidth(enableButton.getX() - getContentX() - 8);
                keybindAndBriefOptions.setPosition(getContentX() + 4, name.getBottom());
                keybindAndBriefOptions.setMaxWidth(enableButton.getX() - getContentX() - 8);
            }

            @Override
            public void extractContent(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
                graphics.fill(getContentX(), getContentY(), getContentX() + getContentWidth(), getContentY() + getContentHeight(), 0x44000000);

                deleteButton.setY(getContentYMiddle() - 10);
                editButton.setY(getContentYMiddle() - 10);
                enableButton.setY(getContentYMiddle() - 10);

                name.setY(getContentY() + 4);
                keybindAndBriefOptions.setY(name.getBottom());

                deleteButton.extractRenderState(graphics, mouseX, mouseY, a);
                editButton.extractRenderState(graphics, mouseX, mouseY, a);
                enableButton.extractRenderState(graphics, mouseX, mouseY, a);

                name.extractRenderState(graphics, mouseX, mouseY, a);
                keybindAndBriefOptions.extractRenderState(graphics, mouseX, mouseY, a);
            }

            @Override
            public @NonNull List<? extends GuiEventListener> children() {
                return List.of(name, keybindAndBriefOptions, enableButton, editButton, deleteButton);
            }

            @Override
            public @NonNull List<? extends NarratableEntry> narratables() {
                return List.of(name, keybindAndBriefOptions, enableButton, editButton, deleteButton);
            }
        }
    }
}
