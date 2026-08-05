package de.pr77pr77.clickguard.client;

import net.minecraft.client.Minecraft;

import static de.pr77pr77.clickguard.client.ClickGuardClient.autoClickingEnabled;

public class Clicker {
    private long nextClickTime = 0; // nanoseconds
    private long clickReleaseTime = 0; // nanoseconds
    private boolean clicking = false;

    public final ConfigManager.ConfigData.Preset preset;

    public Clicker(ConfigManager.ConfigData.Preset preset) {
        this.preset = preset;
    }

    public void handleAutomaticClicks() {
        if (!preset.enabled || !autoClickingEnabled) {
            return;
        }
        long now = System.nanoTime();
        switch (preset.clickingType) {
            case CUSTOM_TIMING:
                if (now >= nextClickTime && !clicking) {
                    startClick();
                    long intervalNanos = preset.customIntervalMS * 1_000_000L;
                    nextClickTime += intervalNanos;
                    // Preventing drift leading to triggering a click right after the current one
                    if (now - nextClickTime > intervalNanos) {
                        nextClickTime = now + intervalNanos;
                    }

                    clickReleaseTime = now + preset.holdingDurationMS * 1_000_000L;
                }

                if (now >= clickReleaseTime && clicking) {
                    releaseClick();
                }
                break;
            case CONTINUOUS:
                Minecraft minecraft = Minecraft.getInstance();
                if (!clicking && minecraft.gui.screen() == null) {
                    startClick(); // Clicking is stopped when the auto clicker is turned off, see releaseClickIfClicking()
                }
                if (minecraft.gui.screen() != null) {
                    releaseClick(); // Screens stop clicks, so we can cleanly release the key and set clicking to false.
                }
                break;
            case COOLDOWN_AWARE:
                if (Minecraft.getInstance().player != null &&
                        Minecraft.getInstance().player.getAttackStrengthScale(1.0F) >= 1.0F && !clicking) {
                    startClick();
                    clickReleaseTime = now + 100 * 1_000_000L; // Max time until release
                }
                if (((Minecraft.getInstance().player != null &&
                        Minecraft.getInstance().player.getAttackStrengthScale(1.0F) < 1.0F)
                        || now >= clickReleaseTime) && clicking) {
                    releaseClick();
                }
        }
    }

    private void startClick() {
        preset.keybind.setDown(true);
        ++preset.keybind.clickCount;
        clicking = true;
    }

    public void releaseClickIfClicking() {
        if (clicking) {
            releaseClick();
        }
    }

    private void releaseClick() {
        preset.keybind.setDown(false);
        clicking = false;
    }
}