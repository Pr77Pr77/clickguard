package de.pr77pr77.clickguard.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonColors;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.math.BigDecimal;
import java.math.RoundingMode;

import static de.pr77pr77.clickguard.ClickGuard.LOGGER;
import static de.pr77pr77.clickguard.ClickGuard.id;
import static de.pr77pr77.clickguard.client.ClickGuardClient.NarratableEntryOfComponent;

public class EditPresetScreen extends Screen {
    private final Screen parent;
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    private OptionsList list;

    private final ConfigManager.ConfigData.Preset preset;

    public EditPresetScreen(Screen parent, ConfigManager.ConfigData.Preset preset) {
        super(Component.translatable("clickguard.presets.edit"));
        this.parent = parent;
        this.preset = preset;
    }

    public EditPresetScreen(Screen parent, KeyMapping key) {
        super(Component.translatable("clickguard.presets.new"));
        this.parent = parent;
        this.preset = new ConfigManager.ConfigData.Preset();
        this.preset.keybind = key;
        ClickGuardClient.configManager.data.presets.add(this.preset);
    }

    @Override
    protected void init() {
        layout.addTitleHeader(title, font);

        list = layout.addToContents(new OptionsList(minecraft, width, layout));
        addOptions();

        layout.addToFooter(Button.builder(CommonComponents.GUI_DONE,
                _ -> onClose()).build());

        layout.visitWidgets(this::addRenderableWidget);
        repositionElements();
    }

    protected void addOptions() {
        list.addEditBox(Component.translatable("clickguard.name"), preset.name, Component.translatable(preset.keybind.getName()),
                (value) -> {
                    if (value.isEmpty()) {
                        preset.name = Component.translatable(preset.keybind.getName()).getString();
                    } else {
                        preset.name = value;
                    }
                });
        list.addButon(Component.translatable("clickguard.keybind.label", Component.translatable(preset.keybind.getName())),
                (_, _) -> minecraft.gui.setScreen(new ChooseKeybindScreen(this, preset)));
        list.addClickingTypeEntry(preset);
        list.addFilterEntry(preset);

        list.addTitleEntry(Component.translatable("clickguard.action.title"));
        if (preset.playerDamaged == null) {
            preset.playerDamaged = new ConfigManager.ConfigData.SimpleAction();
        }
        list.addSimpleActionEntry(Component.translatable("clickguard.action.playerDamaged"), preset.playerDamaged, false);

        if (preset.healthActions == null) {
            preset.healthActions = new ArrayList<>();
        }
        list.addInnerButon(Component.translatable("clickguard.action.health.addButton"), (_, buttonEntry) -> {
            ConfigManager.ConfigData.SliderAction healthAction = new ConfigManager.ConfigData.SliderAction();
            preset.healthActions.add(healthAction);
            list.insertIconSliderActionEntry(healthAction, false,
                    OptionsList.IconSliderActionEntry.Type.HEALTH, list.children().indexOf(buttonEntry) + 1);
        }, false);
        for (ConfigManager.ConfigData.SliderAction healthAction : preset.healthActions) {
            list.addIconSliderActionEntry(healthAction, false, OptionsList.IconSliderActionEntry.Type.HEALTH);
        }

        if (preset.hungerActions == null) {
            preset.hungerActions = new ArrayList<>();
        }
        list.addInnerButon(Component.translatable("clickguard.action.hunger.addButton"), (_, buttonEntry) -> {
            ConfigManager.ConfigData.SliderAction hungerAction = new ConfigManager.ConfigData.SliderAction();
            preset.hungerActions.add(hungerAction);
            list.insertIconSliderActionEntry(hungerAction, false,
                    OptionsList.IconSliderActionEntry.Type.HUNGER, list.children().indexOf(buttonEntry) + 1);
        }, false);
        for (int i = 0; i < preset.hungerActions.size(); i++) {
            list.addIconSliderActionEntry(preset.hungerActions.get(i), false,
                    OptionsList.IconSliderActionEntry.Type.HUNGER);
        }

        if (preset.durabilityActions == null) {
            preset.durabilityActions = new ArrayList<>();
        }
        list.addInnerButon(Component.translatable("clickguard.action.durability.addButton"), (_, buttonEntry) -> {
            ConfigManager.ConfigData.FractionAction durabilityAction = new ConfigManager.ConfigData.FractionAction();
            preset.durabilityActions.add(durabilityAction);
            list.insertFractionSliderEntry(durabilityAction,
                    "clickguard.action.durability.slider",
                    Component.translatable("clickguard.action.durability"), false,
                    list.children().indexOf(buttonEntry) + 1);
        }, false);
        for (int i = 0; i < preset.durabilityActions.size(); i++) {
            list.addFractionSliderEntry(preset.durabilityActions.get(i), "clickguard.action.durability.slider",
                    Component.translatable("clickguard.action.durability"), i == preset.durabilityActions.size() - 1);
        }

        if (preset.waitTimeActions == null) {
            preset.waitTimeActions = new ArrayList<>();
        }
        list.addInnerButon(Component.translatable("clickguard.action.waitTime.addButton"), (_, buttonEntry) -> {
            ConfigManager.ConfigData.TimeAction waitTimeAction = new ConfigManager.ConfigData.TimeAction();
            preset.waitTimeActions.add(waitTimeAction);
            list.insertWaitTimeEntry(waitTimeAction,
                    list.children().indexOf(buttonEntry) + 1 == list.children().size(),
                    list.children().indexOf(buttonEntry) + 1);
            buttonEntry.lastBoxEntry = false; // When an entry was added below, the button can't be the last entry.
        }, preset.waitTimeActions.isEmpty());
        for (int i = 0; i < preset.waitTimeActions.size(); i++) {
            list.addWaitTimeEntry(preset.waitTimeActions.get(i), i == preset.durabilityActions.size() - 1);
        }
    }

    @Override
    public void repositionElements() {
        layout.arrangeElements();
        if (list == null) {
            return;
        }
        list.updateSize(width, layout);
        for (OptionsList.Entry entry : list.children()) {
            entry.init();
        }
    }

    @Override
    public void added() {
        if (list == null) {
            return;
        }
        for (OptionsList.Entry entry : list.children()) {
            entry.screenAdded();
        }
    }

    @Override
    public void onClose() {
        // Remove obsolete slider actions:
        preset.healthActions.removeIf(ConfigManager.ConfigData.SliderAction::isObsolete);
        preset.hungerActions.removeIf(ConfigManager.ConfigData.SliderAction::isObsolete);
        preset.durabilityActions.removeIf(ConfigManager.ConfigData.FractionAction::isObsolete);
        preset.waitTimeActions.removeIf(ConfigManager.ConfigData.TimeAction::isObsolete);

        ClickGuardClient.configManager.save();
        minecraft.gui.setScreen(parent);
    }

    public static class OptionsList extends ContainerObjectSelectionList<OptionsList.Entry> {

        public OptionsList(Minecraft minecraft, int width, HeaderAndFooterLayout layout) {
            super(minecraft, width, layout.getContentHeight(), layout.getHeaderHeight(), 20 + 4);
        }

        public void addButon(Component description, BiConsumer<Button, ButtonEntry> onPress) {
            addEntry(new ButtonEntry(description, onPress));
        }

        public void addEditBox(Component label, String value, Component hint, Consumer<String> responder) {
            addEntry(new EditBoxEntry(minecraft, label, value, hint, responder), 38);
        }

        public void addClickingTypeEntry(ConfigManager.ConfigData.Preset preset) {
            addEntry(new ClickingTypeEntry(minecraft, preset), 2 + 20 + 5 + 10 + 20 + 5 + 10 + 20 + 5 + 10 + 20 + 6);
        }

        public void addFilterEntry(ConfigManager.ConfigData.Preset preset) {
            addEntry(new FilterEntry(minecraft, preset), 55);
        }

        public void addTitleEntry(Component label) {
            addEntry(new TitleEntry(minecraft, label), 4 + minecraft.font.lineHeight);
        }

        public void addSimpleActionEntry(Component label, ConfigManager.ConfigData.SimpleAction action, boolean lastActionEntry) {
            addEntry(new SimpleActionEntry(minecraft, label, action, lastActionEntry), 4 + minecraft.font.lineHeight + 2 + 19 + (lastActionEntry ? 4 : 2));
        }

        public void addIconSliderActionEntry(ConfigManager.ConfigData.SliderAction action, boolean lastActionEntry, IconSliderActionEntry.Type type) {
            IconSliderActionEntry entry = new IconSliderActionEntry(minecraft, action, lastActionEntry, type);
            addEntry(entry, entry.idealHeight);
        }

        public void insertIconSliderActionEntry(ConfigManager.ConfigData.SliderAction action, boolean lastActionEntry, IconSliderActionEntry.Type type, int index) {
            IconSliderActionEntry entry = new IconSliderActionEntry(minecraft, action, lastActionEntry, type);
            insertEntry(index, entry, entry.idealHeight);
            entry.init();
            scrollToEntry(entry);
        }

        public void addInnerButon(Component description, BiConsumer<Button, ButtonEntry> onPress, boolean lastBoxEntry) {
            addEntry(new ButtonEntry(description, onPress, true, lastBoxEntry), 20 + (lastBoxEntry ? 6 : 4));
        }

        public void addFractionSliderEntry(ConfigManager.ConfigData.FractionAction action, String sliderTranslationKey, Component label, boolean lastActionEntry) {
            FractionSliderEntry entry = new FractionSliderEntry(minecraft, action, sliderTranslationKey, label, lastActionEntry);
            addEntry(entry, entry.idealHeight);
        }

        public void insertFractionSliderEntry(ConfigManager.ConfigData.FractionAction action, String sliderTranslationKey, Component label, boolean lastActionEntry, int index) {
            FractionSliderEntry entry = new FractionSliderEntry(minecraft, action, sliderTranslationKey, label, lastActionEntry);
            insertEntry(index, entry, entry.idealHeight);
            entry.init();
            scrollToEntry(entry);
        }

        public void addWaitTimeEntry(ConfigManager.ConfigData.TimeAction action, boolean lastActionEntry) {
            WaitTimeEntry entry = new WaitTimeEntry(minecraft, action, lastActionEntry);
            addEntry(entry, entry.idealHeight);
        }

        public void insertWaitTimeEntry(ConfigManager.ConfigData.TimeAction action, boolean lastActionEntry, int index) {
            WaitTimeEntry entry = new WaitTimeEntry(minecraft, action, lastActionEntry);
            insertEntry(index, entry, entry.idealHeight);
            entry.init();
            scrollToEntry(entry);
        }

        protected void insertEntry(int index, Entry newEntry, int height) {
            List<Entry> displaced = new ArrayList<>();

            for (int i = index; i < children().size(); i++) {
                displaced.add(children().get(i));
            }
            removeEntries(displaced);

            addEntry(newEntry, height);

            for (Entry entry : displaced) {
                addEntry(entry, entry.getHeight());
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

        private static Optional<Integer> parseIntervalToMs(String input) {
            if (input == null) {
                return Optional.empty();
            }
            String string = input.trim();
            if (string.isEmpty()) {
                return Optional.empty();
            }

            // Allow only digits, colon and dot for parsing; remove other characters
            string = string.replaceAll("[^0-9:.]", "");

            // If contains ":" parse as MM:SS(.fff)
            if (string.contains(":")) {
                java.util.regex.Pattern p = java.util.regex.Pattern.compile("^(\\d+):(\\d{1,2})(?:\\.(\\d{1,3}))?$");
                java.util.regex.Matcher m = p.matcher(string);
                if (!m.matches()) {
                    return Optional.empty();
                }
                try {
                    long minutes = Long.parseLong(m.group(1));
                    int seconds = Integer.parseInt(m.group(2));
                    String fracStr = m.group(3);
                    int millis = 0;
                    if (fracStr != null) {
                        int frac = Integer.parseInt(fracStr);
                        int len = fracStr.length();
                        if (len == 1) millis = frac * 100; // .5 -> 500ms
                        else if (len == 2) millis = frac * 10; // .50 -> 500ms
                        else millis = frac; // .500 -> 500ms
                    }
                    long total = minutes * 60000L + (long) seconds * 1000L + millis;
                    if (total < 0 || total > Integer.MAX_VALUE) return Optional.empty();
                    return Optional.of((int) total);
                } catch (NumberFormatException ex) {
                    return Optional.empty();
                }
            }

            // No colon: if pure digits, treat as minutes
            if (string.matches("^\\d+$")) {
                try {
                    long minutes = Long.parseLong(string);
                    long total = minutes * 60000L;
                    if (total < 0 || total > Integer.MAX_VALUE) {
                        return Optional.empty();
                    }
                    return Optional.of((int) total);
                } catch (NumberFormatException ex) {
                    return Optional.empty();
                }
            }

            // fallback:
            return Optional.empty();
        }

        private static String formatMsToInterval(int ms) {
            if (ms < 0) ms = 0;
            long minutes = ms / 60000L;
            int rem = ms % 60000;
            int seconds = rem / 1000;
            int millis = rem % 1000;

            if (seconds == 0 && millis == 0) {
                return String.valueOf(minutes);
            }

            StringBuilder sb = new StringBuilder();
            sb.append(minutes).append(":");
            // seconds always two digits
            sb.append(String.format("%02d", seconds));

            if (millis == 0) return sb.toString();

            String frac;
            if (millis % 100 == 0) {
                // one digit
                frac = Integer.toString(millis / 100);
            } else if (millis % 10 == 0) {
                // two digits, zero-padded
                frac = String.format("%02d", millis / 10);
            } else {
                frac = String.format("%03d", millis);
            }
            sb.append('.').append(frac);
            return sb.toString();
        }

        private static String formatCpsFromMs(int ms) {
            if (ms <= 0) return "0";
            BigDecimal numerator = BigDecimal.valueOf(1000);
            BigDecimal denom = BigDecimal.valueOf(ms);
            // 12 decimal places should be enough to represent very small CPS values; adjust if necessary
            BigDecimal cps = numerator.divide(denom, 12, RoundingMode.HALF_UP).stripTrailingZeros();
            String out = cps.toPlainString();
            // If division produced "0" due to scale, increase precision to avoid losing tiny non-zero values
            if (out.equals("0") && numerator.compareTo(denom) > 0) {
                cps = numerator.divide(denom, 18, RoundingMode.HALF_UP).stripTrailingZeros();
                out = cps.toPlainString();
            }
            return out;
        }

        public abstract static class Entry
                extends ContainerObjectSelectionList.Entry<Entry> {
            abstract void init(); // Initializer after adding, getContentWidth and positions available.

            void screenAdded() {
            }
        }

        public static class ButtonEntry extends Entry {
            private final Button button;
            private boolean innerButton = false; // Button in a box
            private boolean lastBoxEntry = false;

            private ButtonEntry(Component description, BiConsumer<Button, ButtonEntry> onPress) {
                button = Button.builder(description, button -> onPress.accept(button, this)).build(); // Position and size set in init
            }

            private ButtonEntry(Component description, BiConsumer<Button, ButtonEntry> onPress, boolean innerButton, boolean lastBoxEntry) {
                this(description, onPress);
                this.innerButton = innerButton;
                this.lastBoxEntry = lastBoxEntry;
            }

            @Override
            void init() {
                button.setPosition(getContentX() + (innerButton ? 2 : 0), getContentY());
                button.setSize(getContentWidth() - (innerButton ? 4 : 0), getContentHeight() - ((innerButton && lastBoxEntry) ? 2 : 0));
            }

            @Override
            public void extractContent(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
                if (innerButton) {
                    graphics.fill(getContentX(), getY(), getContentRight(), lastBoxEntry ? getContentBottom() : getY() + getHeight(), 0x44000000);
                }
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

        public static class EditBoxEntry extends Entry {
            public final EditBox editBox;
            private final Component label;
            private final Minecraft minecraft;
            private final String initialValue;
            private boolean initialized = false;

            public EditBoxEntry(Minecraft minecraft, Component label, String value, Component hint, Consumer<String> responder) {
                this.minecraft = minecraft;
                this.label = label;
                this.initialValue = value;
                this.editBox = new EditBox(minecraft.font, 0, 0, 0, 20, label); // Position and size set in init
                this.editBox.setHint(hint);
                this.editBox.setResponder(responder);
            }

            @Override
            void init() {
                editBox.setPosition(getContentX() + 2, getContentY() + 2 + minecraft.font.lineHeight + 2);
                editBox.setSize(getContentWidth() - 4, 20);
                if (!initialized) {
                    editBox.setValue(initialValue);
                    initialized = true;
                }
            }

            @Override
            public void extractContent(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
                graphics.fill(getContentX(), getContentY(), getContentRight(), getContentBottom(), 0x44000000);
                graphics.text(
                        minecraft.font,
                        label,
                        getContentX() + 2,
                        getContentY() + 2,
                        0xFFFFFFFF,
                        true
                );
                editBox.setY(getContentY() + 10 + 2);
                editBox.extractRenderState(graphics, mouseX, mouseY, a);
            }

            @Override
            public @NonNull List<? extends GuiEventListener> children() {
                return List.of(editBox);
            }

            @Override
            public @NonNull List<? extends NarratableEntry> narratables() {
                return List.of(editBox);
            }
        }

        public static class ClickingTypeEntry extends Entry {
            private CycleButton<ConfigManager.ConfigData.ClickingType> typeButton;
            final ConfigManager.ConfigData.Preset preset;
            public final EditBox cpsEditBox;
            public final EditBox intervalEditBox;
            public final EditBox durationEditBox;
            public final Tooltip noCustomTimingTooltip;
            private final Minecraft minecraft;
            private boolean initialized = false;
            private boolean suppressCpsUpdate = false;
            private boolean suppressIntervalUpdate = false;

            public ClickingTypeEntry(Minecraft minecraft, ConfigManager.ConfigData.Preset preset) { // Sets the value to preset
                this.minecraft = minecraft;
                this.preset = preset;

                cpsEditBox = new EditBox(minecraft.font, 0, 0, 0, 20, Component.translatable("clickguard.timing.CPS")); // Position and size set in init
                intervalEditBox = new EditBox(minecraft.font, 0, 0, 0, 20, Component.translatable("clickguard.timing.interval")); // Position and size set in init
                durationEditBox = new EditBox(minecraft.font, 0, 0, 0, 20, Component.translatable("clickguard.timing.duration")); // Position and size set in init


                noCustomTimingTooltip = Tooltip.create(Component.translatable("clickguard.timing.noCustomTiming"));

                createTypeButton();

                cpsEditBox.setResponder(value -> {
                    if (suppressCpsUpdate) {
                        cpsEditBox.setTextColor(CommonColors.TEXT_GRAY);
                        return;
                    }
                    if (value.isEmpty()) {
                        return;
                    }
                    String original = value.trim().replace(',', '.');
                    try {
                        double cpsInput = Double.parseDouble(original);
                        long ms;
                        if (cpsInput > 0d) { // Prevent division by zero
                            ms = Math.round(1000.0 / cpsInput);
                        } else {
                            ms = 0; // Later changed to 1, Color red in EditBox.
                        }

                        if (ms <= 0) {
                            cpsEditBox.setTextColor(0xFFFC5454);
                            ms = 1;
                        } else if (ms > Integer.MAX_VALUE) {
                            cpsEditBox.setTextColor(0xFFFC5454);
                            ms = Integer.MAX_VALUE;
                        } else {
                            cpsEditBox.setTextColor(CommonColors.TEXT_GRAY);
                        }

                        // Update preset and interval display, but do NOT overwrite user's CPS input to avoid surprising edits while typing
                        if (ms != this.preset.customIntervalMS) {
                            this.preset.customIntervalMS = (int) ms;
                            String intervalFormatted = formatMsToInterval((int) ms);
                            if (!intervalFormatted.equals(intervalEditBox.getValue())) {
                                suppressIntervalUpdate = true;
                                intervalEditBox.setValue(intervalFormatted);
                                suppressIntervalUpdate = false;
                            }
                            checkDurationTooLong(preset.holdingDurationMS);
                        }
                    } catch (NumberFormatException ignored) {
                        LOGGER.warn("Invalid input for CPS: " + value);
                        // On invalid input, restore a safe formatted CPS value
                        String formatted = formatCpsFromMs(this.preset.customIntervalMS);
                        if (!formatted.equals(value)) {
                            suppressCpsUpdate = true;
                            cpsEditBox.setValue(formatted);
                            suppressCpsUpdate = false;
                        }
                    }
                });
                intervalEditBox.setResponder(value -> {
                    if (suppressIntervalUpdate) {
                        intervalEditBox.setTextColor(CommonColors.TEXT_GRAY);
                        return;
                    }
                    if (value.isEmpty()) {
                        return;
                    }
                    Optional<Integer> ms = parseIntervalToMs(value);
                    if (ms.isPresent() && ms.get() != this.preset.customIntervalMS && ms.get() > 0) {
                        this.preset.customIntervalMS = ms.get();

                        String formatted = formatCpsFromMs(ms.get());
                        if (!formatted.equals(cpsEditBox.getValue())) {
                            suppressCpsUpdate = true;
                            cpsEditBox.setValue(formatted);
                            suppressCpsUpdate = false;
                        }
                        checkDurationTooLong(preset.holdingDurationMS);
                        intervalEditBox.setTextColor(CommonColors.TEXT_GRAY);
                    } else if (ms.isPresent() && ms.get() > 0) {
                        intervalEditBox.setTextColor(CommonColors.TEXT_GRAY);
                    } else {
                        intervalEditBox.setTextColor(0xFFFC5454);
                    }

                    String cleaned = value.trim().replaceAll("[^0-9:.]", "");
                    if (!value.equals(cleaned)) {
                        suppressIntervalUpdate = true;
                        intervalEditBox.setValue(cleaned);
                        suppressIntervalUpdate = false;
                    }
                });
                durationEditBox.setResponder(value -> {
                    if (value.isEmpty()) {
                        return;
                    }
                    String cleaned = value.trim().replaceAll("[^0-9]", "");
                    if (!value.equals(cleaned)) {
                        durationEditBox.setValue(cleaned);
                    }

                    int duration;
                    try {
                        duration = Integer.parseInt(cleaned);
                    } catch (NumberFormatException ignored) {
                        LOGGER.warn("Invalid input for duration: " + value);
                        return;
                    }

                    checkDurationTooLong(duration);
                    preset.holdingDurationMS = duration;
                });

                changeEditBoxes(this.preset.clickingType);
            }

            void createTypeButton() {
                typeButton = CycleButton.builder(ConfigManager.ConfigData.ClickingType::getComponent,
                                this.preset.clickingType)
                        .withValues(preset.keybind.getName().equals("key.attack") ? Arrays.asList(ConfigManager.ConfigData.ClickingType.values()) :
                                List.of(ConfigManager.ConfigData.ClickingType.CUSTOM_TIMING, ConfigManager.ConfigData.ClickingType.CONTINUOUS))
                        .create(0, 0, 0, 20, Component.translatable("clickguard.type.description"),
                                (_, value) -> {
                                    this.preset.clickingType = value;
                                    changeEditBoxes(value);
                                }); // Position and size set in init
            }

            void changeEditBoxes(ConfigManager.ConfigData.ClickingType value) {
                if (value == ConfigManager.ConfigData.ClickingType.CUSTOM_TIMING) {
                    cpsEditBox.active = true;
                    intervalEditBox.active = true;
                    durationEditBox.active = true;

                    cpsEditBox.setTooltip(null);
                    intervalEditBox.setTooltip(null);
                    durationEditBox.setTooltip(null);
                } else {
                    cpsEditBox.active = false;
                    intervalEditBox.active = false;
                    durationEditBox.active = false;

                    cpsEditBox.setTooltip(noCustomTimingTooltip);
                    intervalEditBox.setTooltip(noCustomTimingTooltip);
                    durationEditBox.setTooltip(noCustomTimingTooltip);
                }
            }

            @Override
            void screenAdded() {
                createTypeButton();
            }

            @Override
            void init() {
                int currentY = getContentY() + 2;
                typeButton.setPosition(getContentX() + 2, currentY);
                typeButton.setSize(getContentWidth() - 4, 20);
                currentY += 20 + 5;

                currentY += 10; // Label
                cpsEditBox.setPosition(getContentX() + 2, currentY);
                cpsEditBox.setSize(getContentWidth() - 5, 20);
                currentY += 20 + 5;

                currentY += 10; // Label
                intervalEditBox.setPosition(getContentX() + 2, currentY);
                intervalEditBox.setSize(getContentWidth() - 5, 20);
                currentY += 20 + 5;

                currentY += 10; // Label
                durationEditBox.setPosition(getContentX() + 2, currentY);
                durationEditBox.setSize(getContentWidth() - 5, 20);

                if (!initialized) {
                    cpsEditBox.setValue(formatCpsFromMs(this.preset.customIntervalMS));
                    intervalEditBox.setValue(formatMsToInterval(this.preset.customIntervalMS));
                    durationEditBox.setValue(String.valueOf(this.preset.holdingDurationMS));
                    initialized = true;
                }
            }

            private void checkDurationTooLong(int duration) {
                if (duration >= preset.customIntervalMS) {
                    duration = preset.customIntervalMS - 1;
                    durationEditBox.setValue(String.valueOf(duration));
                    LOGGER.warn("Duration is too high (>= interval): " + duration);
                }
            }

            @Override
            public void extractContent(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
                graphics.fill(getContentX(), getContentY(), getContentRight(), getContentBottom(), 0x44000000);

                typeButton.setY(getContentY() + 2);
                typeButton.extractRenderState(graphics, mouseX, mouseY, a);

                graphics.text(
                        minecraft.font,
                        Component.translatable("clickguard.timing.CPS"),
                        getContentX() + 2,
                        getContentY() + 2 + 20 + 5,
                        0xFFFFFFFF,
                        true
                );
                cpsEditBox.setY(getContentY() + 2 + 20 + 5 + 10);
                cpsEditBox.extractRenderState(graphics, mouseX, mouseY, a);

                graphics.text(
                        minecraft.font,
                        Component.translatable("clickguard.timing.interval"),
                        getContentX() + 2,
                        getContentY() + 2 + 20 + 5 + 10 + 20 + 5,
                        0xFFFFFFFF,
                        true
                );
                intervalEditBox.setY(getContentY() + 2 + 20 + 5 + 10 + 20 + 5 + 10);
                intervalEditBox.extractRenderState(graphics, mouseX, mouseY, a);

                graphics.text(
                        minecraft.font,
                        Component.translatable("clickguard.timing.duration"),
                        getContentX() + 2,
                        getContentY() + 2 + 20 + 5 + 10 + 20 + 5 + 10 + 20 + 5,
                        0xFFFFFFFF,
                        true
                );
                durationEditBox.setY(getContentY() + 2 + 20 + 5 + 10 + 20 + 5 + 10 + 20 + 5 + 10);
                durationEditBox.extractRenderState(graphics, mouseX, mouseY, a);
            }

            @Override
            public @NonNull List<? extends GuiEventListener> children() {
                return List.of(typeButton, cpsEditBox, intervalEditBox, durationEditBox);
            }

            @Override
            public @NonNull List<? extends NarratableEntry> narratables() {
                return List.of(typeButton, cpsEditBox, intervalEditBox, durationEditBox);
            }
        }

        public static class FilterEntry extends Entry {
            public final Checkbox checkboxEntities;
            public final Checkbox checkboxBlocks;
            private final Minecraft minecraft;

            public FilterEntry(Minecraft minecraft, ConfigManager.ConfigData.Preset preset) { // Sets the value to preset
                this.minecraft = minecraft;
                checkboxEntities = Checkbox.builder(Component.translatable("clickguard.filter.entities"), minecraft.font)
                        .selected(preset.filterEntities).onValueChange((_, value) -> preset.filterEntities = value).build();
                checkboxBlocks = Checkbox.builder(Component.translatable("clickguard.filter.blocks"), minecraft.font)
                        .selected(preset.filterBlocks).onValueChange((_, value) -> preset.filterBlocks = value).build();
            }

            @Override
            void init() {
                checkboxEntities.setPosition(getContentX() + 2, getContentY() + 2 + minecraft.font.lineHeight + 2);
                checkboxEntities.setWidth(getContentWidth() - 4);
                checkboxBlocks.setPosition(getContentX() + 2, checkboxEntities.getBottom() + 2);
                checkboxBlocks.setWidth(getContentWidth() - 4);
            }

            @Override
            public void extractContent(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
                graphics.fill(getContentX(), getContentY(), getContentRight(), getContentBottom(), 0x44000000);
                graphics.text(
                        minecraft.font,
                        Component.translatable("clickguard.filter.title"),
                        getContentX() + 2,
                        getContentY() + 2,
                        0xFFFFFFFF,
                        true
                );

                checkboxEntities.setY(getContentY() + 2 + minecraft.font.lineHeight + 2);
                checkboxEntities.setWidth(getContentWidth() - 4);
                checkboxEntities.extractContents(graphics, mouseX, mouseY, a);

                checkboxBlocks.setY(checkboxEntities.getBottom() + 2);
                checkboxBlocks.setWidth(getContentWidth() - 4);
                checkboxBlocks.extractContents(graphics, mouseX, mouseY, a);
            }

            @Override
            public @NonNull List<? extends GuiEventListener> children() {
                return List.of(checkboxEntities, checkboxBlocks);
            }

            @Override
            public @NonNull List<? extends NarratableEntry> narratables() {
                return List.of(NarratableEntryOfComponent(Component.translatable("clickguard.filter.title")), checkboxEntities, checkboxBlocks);
            }
        }

        public static class TitleEntry extends Entry { // Connects at the bottom
            private final Minecraft minecraft;
            public final Component label;

            TitleEntry(Minecraft minecraft, Component label) {
                this.minecraft = minecraft;
                this.label = label;
            }

            @Override
            void init() {
            }

            @Override
            public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
                graphics.fill(getContentX(), getContentY(), getContentRight(), getY() + getHeight(), 0x44000000);
                graphics.text(
                        minecraft.font,
                        label,
                        getContentX() + 2,
                        getContentY() + 2,
                        0xFFFFFFFF,
                        true
                );
            }

            @Override
            public @NonNull List<? extends GuiEventListener> children() {
                return List.of();
            }

            @Override
            public @NonNull List<? extends NarratableEntry> narratables() {
                return List.of(NarratableEntryOfComponent(label));
            }
        }

        public static abstract class AbstractActionEntry extends Entry {
            public final Checkbox checkboxStopClicker;
            public final Checkbox checkboxNotification;
            public final Checkbox checkboxLeave;

            public final Component label;

            public final boolean lastActionEntry;

            protected final Minecraft minecraft;

            public AbstractActionEntry(Minecraft minecraft, Component label, ConfigManager.ConfigData.SimpleAction action, boolean lastActionEntry) {
                this.minecraft = minecraft;
                checkboxStopClicker = Checkbox.builder(Component.translatable("clickguard.action.option.stopClicker"), minecraft.font)
                        .selected(action.stopClicker).onValueChange((_, value) -> action.stopClicker = value).build();
                checkboxNotification = Checkbox.builder(Component.translatable("clickguard.action.option.notification"), minecraft.font)
                        .selected(action.notification).onValueChange((_, value) -> action.notification = value).build();
                checkboxLeave = Checkbox.builder(Component.translatable("clickguard.action.option.leave"), minecraft.font)
                        .selected(action.leaveWorld).onValueChange((_, value) -> action.leaveWorld = value).build();
                this.label = label;
                this.lastActionEntry = lastActionEntry;
            }

            void initCheckboxes(int checkboxesY) {
                final int checkboxesWidth = (getContentWidth() - 12) / 3;
                checkboxStopClicker.setPosition(getContentX() + 4, checkboxesY);
                checkboxStopClicker.setWidth(checkboxesWidth);
                checkboxNotification.setPosition(getContentX() + 4 + checkboxesWidth + 2, checkboxesY);
                checkboxNotification.setWidth(checkboxesWidth);
                checkboxLeave.setPosition(getContentX() + 4 + (checkboxesWidth + 2) * 2, checkboxesY);
                checkboxLeave.setWidth(checkboxesWidth);
            }

            public void extractBase(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, int checkboxesY) {
                // General box, together with other
                graphics.fill(getContentX(), getY(), getContentRight(), lastActionEntry ? getContentBottom() : getY() + getHeight(), 0x44000000);
                // Own box:
                graphics.fill(getContentX() + 2, getY() + 2, getContentRight() - 2, lastActionEntry ? (getContentBottom() - 2) : getContentBottom(), 0x44000000);
                graphics.text(
                        minecraft.font,
                        label,
                        getContentX() + 4,
                        getContentY() + 2,
                        0xFFFFFFFF,
                        true
                );

                checkboxStopClicker.setY(checkboxesY);
                checkboxStopClicker.extractContents(graphics, mouseX, mouseY, a);

                checkboxNotification.setY(checkboxesY);
                checkboxNotification.extractContents(graphics, mouseX, mouseY, a);

                checkboxLeave.setY(checkboxesY);
                checkboxLeave.extractContents(graphics, mouseX, mouseY, a);
            }
        }

        public static class SimpleActionEntry extends AbstractActionEntry {
            public SimpleActionEntry(Minecraft minecraft, Component label, ConfigManager.ConfigData.SimpleAction action, boolean lastActionEntry) {
                super(minecraft, label, action, lastActionEntry);
            }

            @Override
            void init() {
                initCheckboxes(getContentY() + 2 + minecraft.font.lineHeight + 2);
            }

            @Override
            public void extractContent(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
                extractBase(graphics, mouseX, mouseY, a, getContentY() + 2 + minecraft.font.lineHeight + 2);
            }

            @Override
            public @NonNull List<? extends GuiEventListener> children() {
                return List.of(checkboxStopClicker, checkboxNotification, checkboxLeave);
            }

            @Override
            public @NonNull List<? extends NarratableEntry> narratables() {
                return List.of(NarratableEntryOfComponent(label), checkboxStopClicker, checkboxNotification, checkboxLeave);
            }
        }

        public static class IconSliderActionEntry extends AbstractActionEntry {
            public final int idealHeight;

            final Type type;
            IconSlider slider;

            public enum Type {
                HEALTH(new IconSlider.IconSprites(Identifier.withDefaultNamespace("hud/heart/full"),
                        Identifier.withDefaultNamespace("hud/heart/container"),
                        Identifier.withDefaultNamespace("hud/heart/half")),
                        "clickguard.action.health.slider",
                        Component.translatable("clickguard.action.health")),
                HUNGER(new IconSlider.IconSprites(Identifier.withDefaultNamespace("hud/food_full"),
                        Identifier.withDefaultNamespace("hud/food_empty"),
                        id("hud/food_right_eaten")),
                        "clickguard.action.hunger.slider",
                        Component.translatable("clickguard.action.hunger"));

                final IconSlider.IconSprites sprites;
                final String sliderTranslationKey;
                final Component label;

                Type(IconSlider.IconSprites sprites, String sliderTranslationKey, Component label) {
                    this.sprites = sprites;
                    this.sliderTranslationKey = sliderTranslationKey;
                    this.label = label;
                }
            }

            public IconSliderActionEntry(Minecraft minecraft, ConfigManager.ConfigData.SliderAction action, boolean lastActionEntry,
                                         Type type) {
                super(minecraft, type.label, action, lastActionEntry);
                this.type = type;
                slider = new IconSlider(0, 0, type.sprites, 20,
                        type.sliderTranslationKey, action.points, points -> action.points = points);
                idealHeight = 4 + minecraft.font.lineHeight + 2 + IconSlider.HEIGHT + 2 + 19 + (lastActionEntry ? 4 : 2);
            }

            @Override
            void init() {
                slider.setPosition(getContentX() + 4, getContentY() + 2 + minecraft.font.lineHeight + 2);

                initCheckboxes(getContentY() + 2 + minecraft.font.lineHeight + 2 + slider.getHeight() + 2);
            }

            @Override
            public void extractContent(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
                extractBase(graphics, mouseX, mouseY, a, getContentY() + 2 + minecraft.font.lineHeight + 2 + slider.getHeight() + 2);

                slider.setY(getContentY() + 2 + minecraft.font.lineHeight + 2);
                slider.extractRenderState(graphics, mouseX, mouseY, a);
            }

            @Override
            public @NonNull List<? extends GuiEventListener> children() {
                return List.of(slider, checkboxStopClicker, checkboxNotification, checkboxLeave);
            }

            @Override
            public @NonNull List<? extends NarratableEntry> narratables() {
                return List.of(NarratableEntryOfComponent(type.label), slider, checkboxStopClicker, checkboxNotification, checkboxLeave);
            }
        }

        public static class FractionSliderEntry extends AbstractActionEntry {
            public final int idealHeight;

            FractionSlider slider;

            public FractionSliderEntry(Minecraft minecraft, ConfigManager.ConfigData.FractionAction action, String sliderTranslationKey,
                                       Component label, boolean lastActionEntry) {
                super(minecraft, label, action, lastActionEntry);
                slider = new FractionSlider(0, 0, 0, AbstractSliderButton.DEFAULT_HEIGHT,
                        sliderTranslationKey, action.fraction, points -> action.fraction = points);
                idealHeight = 4 + minecraft.font.lineHeight + 2 + slider.getHeight() + 2 + 19 + (lastActionEntry ? 4 : 2);
            }

            @Override
            void init() {
                slider.setPosition(getContentX() + 4, getContentY() + 2 + minecraft.font.lineHeight + 2);
                slider.setWidth(getContentWidth() - 8);

                initCheckboxes(getContentY() + 2 + minecraft.font.lineHeight + 2 + slider.getHeight() + 2);
            }

            @Override
            public void extractContent(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
                extractBase(graphics, mouseX, mouseY, a, getContentY() + 2 + minecraft.font.lineHeight + 2 + slider.getHeight() + 2);

                slider.setY(getContentY() + 2 + minecraft.font.lineHeight + 2);
                slider.extractRenderState(graphics, mouseX, mouseY, a);
            }

            @Override
            public @NonNull List<? extends GuiEventListener> children() {
                return List.of(slider, checkboxStopClicker, checkboxNotification, checkboxLeave);
            }

            @Override
            public @NonNull List<? extends NarratableEntry> narratables() {
                return List.of(NarratableEntryOfComponent(label), slider, checkboxStopClicker, checkboxNotification, checkboxLeave);
            }
        }

        public static class WaitTimeEntry extends AbstractActionEntry {
            public final int idealHeight;

            EditBox waitTimeEditBox;

            public WaitTimeEntry(Minecraft minecraft, ConfigManager.ConfigData.TimeAction action, boolean lastActionEntry) {
                super(minecraft, Component.translatable("clickguard.action.waitTime"), action, lastActionEntry);
                waitTimeEditBox = new EditBox(minecraft.font, Component.translatable("clickguard.action.waitTime"));
                waitTimeEditBox.setHint(Component.translatable("clickguard.action.waitTime.hint"));
                waitTimeEditBox.setResponder(value -> {
                    if (value.isEmpty()) {
                        action.timeMS = null;
                        return;
                    }
                    Optional<Integer> ms = parseIntervalToMs(value);
                    if (ms.isPresent() && !ms.get().equals(action.timeMS) && ms.get() > 1) {
                        action.timeMS = ms.get();

                        waitTimeEditBox.setTextColor(CommonColors.TEXT_GRAY);
                    } else if (ms.isPresent() && ms.get() > 1) {
                        waitTimeEditBox.setTextColor(CommonColors.TEXT_GRAY);
                    } else {
                        waitTimeEditBox.setTextColor(0xFFFC5454);
                        action.timeMS = null;
                    }

                    String cleaned = value.trim().replaceAll("[^0-9:.]", "");
                    if (!value.equals(cleaned)) {
                        waitTimeEditBox.setValue(cleaned);
                    }
                });
                if (action.timeMS != null) {
                    waitTimeEditBox.setValue(formatMsToInterval(action.timeMS));
                }
                idealHeight = 4 + minecraft.font.lineHeight + 2 + 20 + 2 + 19 + (lastActionEntry ? 4 : 2);
            }

            @Override
            void init() {
                waitTimeEditBox.setRectangle(getContentWidth() - 8, 20,
                        getContentX() + 4, getContentY() + 2 + minecraft.font.lineHeight + 2);

                initCheckboxes(getContentY() + 2 + minecraft.font.lineHeight + 2 + waitTimeEditBox.getHeight() + 2);
            }

            @Override
            public void extractContent(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
                extractBase(graphics, mouseX, mouseY, a, getContentY() + 2 + minecraft.font.lineHeight + 2 + waitTimeEditBox.getHeight() + 2);

                waitTimeEditBox.setY(getContentY() + 2 + minecraft.font.lineHeight + 2);
                waitTimeEditBox.extractRenderState(graphics, mouseX, mouseY, a);
            }

            @Override
            public @NonNull List<? extends GuiEventListener> children() {
                return List.of(waitTimeEditBox, checkboxStopClicker, checkboxNotification, checkboxLeave);
            }

            @Override
            public @NonNull List<? extends NarratableEntry> narratables() {
                return List.of(NarratableEntryOfComponent(label), waitTimeEditBox, checkboxStopClicker, checkboxNotification, checkboxLeave);
            }
        }
    }
}