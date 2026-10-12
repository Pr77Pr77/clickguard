package de.pr77pr77.clickguard.client;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.HolderLookup.Provider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static de.pr77pr77.clickguard.ClickGuard.LOGGER;

public class ConfigManager {
    private final Path filePath;
    public ConfigData data;

    private Gson gson;

    ConfigManager() {
        filePath = FabricLoader.getInstance().getConfigDir().resolve("clickguard.json");
        ClientLifecycleEvents.CLIENT_STARTED.register(minecraft -> {
            gson = new GsonBuilder()
                    .registerTypeAdapter(KeyMapping.class, new KeyMappingAdapter())
                    .registerTypeAdapter(ItemStack.class, new ItemStackTypeAdapter(VanillaRegistries.createLookup(), true))
                    .setPrettyPrinting().create();
            load();
        });
        ClientPlayConnectionEvents.JOIN.register((connection, packetSender, minecraft) -> {
            gson = new GsonBuilder()
                    .registerTypeAdapter(KeyMapping.class, new KeyMappingAdapter())
                    .registerTypeAdapter(ItemStack.class, new ItemStackTypeAdapter(connection.registryAccess()))
                    .setPrettyPrinting().create();
            load();
        });
    }

    public static class ConfigData {
        public List<Preset> presets = new ArrayList<>();

        public boolean allowClickingWhenEnabled = true;
        public boolean keepEnabledAfterDisconnect = false;

        public static class Preset {
            public String name = "";
            public KeyMapping keybind;

            public ClickingType clickingType = ClickingType.CUSTOM_TIMING;
            public int customIntervalMS = 100; // 100 = 10 CPS
            public int holdingDurationMS = 40;
            public FirstClickMoment firstClickMoment = FirstClickMoment.DIRECTLY;

            public boolean filterEntities = false;
            public boolean filterBlocks = false;

            public List<ItemStack> mainHandFilters;
            public List<ItemStack> offHandFilters;

            public SimpleAction playerDamaged = new SimpleAction();

            public List<SliderAction> healthActions = new ArrayList<>();
            public List<SliderAction> hungerActions = new ArrayList<>();

            public List<FractionAction> durabilityActions = new ArrayList<>();

            public List<TimeAction> waitTimeActions = new ArrayList<>();

            public boolean enabled = false;
        }

        public enum ClickingType {
            CONTINUOUS("clickguard.type.continuous"),
            CUSTOM_TIMING("clickguard.type.customTiming"),
            COOLDOWN_AWARE("clickguard.type.cooldownAware");

            public final String translationKey;

            ClickingType(String translationKey) {
                this.translationKey = translationKey;
            }

            public Component getComponent() {
                return Component.translatable(translationKey);
            }
        }

        public enum FirstClickMoment{
            DIRECTLY("clickguard.timing.firstClick.directly"),
            AFTER_INTERVAL("clickguard.timing.firstClick.afterInterval");

            public final String translationKey;

            FirstClickMoment(String translationKey) {
                this.translationKey = translationKey;
            }

            public Component getComponent() {
                return Component.translatable(translationKey);
            }
        }

        public static class SimpleAction {
            public boolean stopClicker = false;
            public boolean notification = false;
            public boolean leaveWorld = false;

            public transient boolean triggered = false; // If an action is triggered on start, it will be ignored.
        }

        public static class SliderAction extends SimpleAction {
            public int points = 0;

            public boolean isObsolete() {
                return !stopClicker && !notification && !leaveWorld;
            }
        }

        public static class FractionAction extends SimpleAction {
            public double fraction = 0;

            public boolean isObsolete() {
                return !stopClicker && !notification && !leaveWorld;
            }
        }

        public static class TimeAction extends SimpleAction {
            public Integer timeMS; // null = not set

            public boolean isObsolete() {
                return (!stopClicker && !notification && !leaveWorld) || timeMS == null;
            }
        }
    }

    public static class KeyMappingAdapter extends TypeAdapter<KeyMapping> {

        @Override
        public void write(JsonWriter out, KeyMapping value) throws IOException {
            if (value == null) {
                out.nullValue();
                return;
            }
            out.value(value.getName());
        }

        @Override
        public KeyMapping read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            String name = in.nextString();
            return Arrays.stream(Minecraft.getInstance().options.keyMappings)
                    .filter((KeyMapping keyMapping) -> keyMapping.getName().equals(name))
                    .findFirst().orElse(null);
        }
    }

    private void load() {
        try {
            Files.createDirectories(filePath.getParent());
            if (Files.exists(filePath)) {
                String json = Files.readString(filePath);
                data = gson.fromJson(json, ConfigData.class);
                if (data == null) {
                    data = new ConfigData();
                }
            } else {
                this.data = new ConfigData();
                save();
            }
        } catch (IOException e) {
            e.printStackTrace();
            this.data = new ConfigData();
        }
    }

    public void save() {
        String json;
        synchronized (this) {
            json = gson.toJson(data);
        }
        CompletableFuture.runAsync(() -> {
            try {
                Files.createDirectories(filePath.getParent());
                Files.writeString(filePath, json);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public static class ItemStackTypeAdapter extends TypeAdapter<ItemStack> {
        private final DynamicOps<JsonElement> ops;

        private final boolean readingErrorLowPriority;

        public ItemStackTypeAdapter(Provider registries) {
            this(registries, false);
        }

        public ItemStackTypeAdapter(Provider registries, boolean readingErrorLowPriority) {
            this.ops = registries.createSerializationContext(JsonOps.INSTANCE);
            this.readingErrorLowPriority = readingErrorLowPriority;
        }

        @Override
        public void write(JsonWriter out, ItemStack stack) throws IOException {
            if (stack == null || stack.isEmpty()) {
                out.nullValue();
                return;
            }

            DataResult<JsonElement> result = ItemStack.CODEC.encodeStart(ops, stack);
            if (result.isSuccess()) {
                out.jsonValue(result.getOrThrow().toString());
            } else if (result.error().isPresent()) {
                LOGGER.error("Could not save an ItemStack: " + result.error().get().message());
                out.nullValue();
            } else {
                LOGGER.error("Could not save an ItemStack due to an unknown error.");
            }
        }

        @Override
        public ItemStack read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return ItemStack.EMPTY;
            }

            JsonElement json = JsonParser.parseReader(in);
            DataResult<ItemStack> result = ItemStack.CODEC.parse(ops, json);
            if (result.isSuccess()) {
                return result.getOrThrow();
            } else if (result.error().isPresent()) {
                writeReadingErrorLog("Could not load an ItemStack: " + result.error().get().message());
            } else {
                writeReadingErrorLog("Could not load an ItemStack due to an unknown error.");
            }
            return ItemStack.EMPTY;
        }

        private void writeReadingErrorLog(String message) {
            if (readingErrorLowPriority) {
                LOGGER.info(message);
            } else {
                LOGGER.error(message);
            }
        }
    }
}
