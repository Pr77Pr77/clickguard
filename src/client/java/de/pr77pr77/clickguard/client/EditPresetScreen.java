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
import net.minecraft.util.CommonColors;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.math.BigDecimal;
import java.math.RoundingMode;

import static de.pr77pr77.clickguard.ClickGuard.LOGGER;

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
        this.preset = ClickGuardClient.configManager.data.new Preset();
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
                _ -> minecraft.gui.setScreen(new ChooseKeybindScreen(this, preset)));
        list.addClickingTypeEntry(preset);
    }

    @Override
    public void repositionElements() {
        layout.arrangeElements();
        if (list != null) list.updateSize(width, layout);
        assert list != null;
        for (OptionsList.Entry entry : list.children()) {
            entry.init();
        }
    }

    @Override
    public void onClose() {
        ClickGuardClient.configManager.save();
        minecraft.gui.setScreen(parent);
    }

    public static class OptionsList extends ContainerObjectSelectionList<OptionsList.Entry> {

        public OptionsList(Minecraft minecraft, int width, HeaderAndFooterLayout layout) {
            super(minecraft, width, layout.getContentHeight(), layout.getHeaderHeight(), 20 + 8);
        }

        public void addButon(Component description, Button.OnPress onPress) {
            addEntry(new ButtonEntry(description, onPress));
        }

        public void addEditBox(Component label, String value, Component hint, Consumer<String> responder) {
            addEntry(new EditBoxEntry(minecraft, label, value, hint, responder), 38);
        }

        public void addClickingTypeEntry(ConfigManager.ConfigData.Preset preset) {
            addEntry(new ClickingTypeEntry(minecraft, preset), 2 + 20 + 5 + 10 + 20 + 5 + 10 + 20 + 5 + 10 + 20 + 6);
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
        }

        public static class ButtonEntry extends Entry {
            private final Button button;

            private ButtonEntry(Component description, Button.OnPress onPress) {
                button = Button.builder(description, onPress).build(); // Position and size set in init
            }

            @Override
            void init() {
                button.setPosition(getContentX() + 2, getContentY() + 2);
                button.setSize(getContentWidth() - 4, getContentHeight() - 4);
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
                editBox.setPosition(getContentX() + 2, getContentY());
                editBox.setSize(getContentWidth() - 4, 20);
                if (!initialized) {
                    editBox.setValue(initialValue);
                    initialized = true;
                }
            }

            @Override
            public void extractContent(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
                graphics.fill(getContentX(), getContentY(), getContentX() + getContentWidth(), getContentY() + getContentHeight(), 0x44000000);
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
            private final CycleButton<ConfigManager.ConfigData.ClickingType> typeButton;
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

                typeButton = CycleButton.builder(ConfigManager.ConfigData.ClickingType::getComponent,
                                this.preset.clickingType)
                        .withValues(ConfigManager.ConfigData.ClickingType.values())
                        .create(0, 0, 0, 20, Component.translatable("clickguard.type.description"),
                                (_, value) -> {
                                    this.preset.clickingType = value;
                                    changeEditBoxes(value);
                                }); // Position and size set in init

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
                    String cleaned = value.trim().replaceAll("[^0-9:.]", "");
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
                graphics.fill(getContentX(), getContentY(), getContentX() + getContentWidth(), getContentY() + getContentHeight(), 0x44000000);

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
    }
}