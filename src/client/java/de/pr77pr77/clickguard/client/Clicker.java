package de.pr77pr77.clickguard.client;

public class Clicker {
    private long nextClickTime = 0; // nanoseconds
    private long clickReleaseTime = 0; // nanoseconds
    private boolean clicking = false;

    public final ConfigManager.ConfigData.Preset preset;

    public Clicker(ConfigManager.ConfigData.Preset preset) {
        this.preset = preset;
    }

    public void handleAutomaticClicks() {
        long now = System.nanoTime();
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