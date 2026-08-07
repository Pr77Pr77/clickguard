package de.pr77pr77.clickguard.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ConfigManager {
    private final Path filePath;
    public ConfigData data;

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(KeyMapping.class, new KeyMappingAdapter())
            .setPrettyPrinting().create();

    ConfigManager() {
        filePath = FabricLoader.getInstance().getConfigDir().resolve("clickguard.json");
        ClientLifecycleEvents.CLIENT_STARTED.register(_ -> load());
    }

    public class ConfigData {
        List<Preset> presets = new ArrayList<>();

        public class Preset {
            public String name = "";
            public KeyMapping keybind;

            public ClickingType clickingType = ClickingType.CUSTOM_TIMING;
            public int customIntervalMS = 100; // 100 = 10 CPS
            public int holdingDurationMS = 40;

            public boolean filterEntities = false;
            public boolean filterBlocks = false;

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
                data = GSON.fromJson(json, ConfigData.class);
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
            json = GSON.toJson(data);
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
}
