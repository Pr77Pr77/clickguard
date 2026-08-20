package de.pr77pr77.clickguard.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

import static de.pr77pr77.clickguard.client.ClickGuardClient.autoClickingEnabled;
import static de.pr77pr77.clickguard.client.ClickGuardClient.formatDuration;

public class Clicker {
    private long nextClickTime = 0; // nanoseconds
    private long clickReleaseTime = 0; // nanoseconds
    private boolean clicking = false;

    private long lastClickTime; // nanoseconds

    public final ConfigManager.ConfigData.Preset preset;

    public Clicker(ConfigManager.ConfigData.Preset preset) {
        this.preset = preset;

        // Check every health and hunger action to prevent triggering on startup
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            preset.healthActions
                    .forEach(action -> action.triggered = player.getHealth() <= action.points);
            preset.hungerActions
                    .forEach(action -> action.triggered = player.getFoodData().getFoodLevel() <= action.points);
            preset.durabilityActions
                    .forEach(action -> action.triggered =
                            1d - ((double) player.getMainHandItem().getDamageValue() / player.getMainHandItem().getMaxDamage()) <= action.fraction);
            preset.waitTimeActions
                    .forEach(action -> action.triggered = false);
        }
        lastClickTime = System.nanoTime();

        preset.playerDamaged.triggered = false;
    }

    public void handleAutomaticClicks() { // Returns weather the clicker should be stopped
        if (!preset.enabled || !autoClickingEnabled) {
            return;
        }
        long now = System.nanoTime();
        switch (preset.clickingType) {
            case CUSTOM_TIMING:
                if (now >= nextClickTime && !clicking) {
                    startClick();
                    long intervalNanos = preset.customIntervalMS * 1_000_000L;
                    nextClickTime = now + intervalNanos;

                    clickReleaseTime = now + preset.holdingDurationMS * 1_000_000L;
                }

                if (now >= clickReleaseTime && clicking) {
                    releaseClick();
                }
                break;
            case CONTINUOUS:
                Minecraft minecraft = Minecraft.getInstance();
                if (!clicking && minecraft.screen == null) {
                    startClick(); // Clicking is stopped when the auto clicker is turned off, see releaseClickIfClicking()
                }
                if (minecraft.screen != null) {
                    releaseClick(); // Screens stop clicks, so we can cleanly release the key and set clicking to false.
                }
                if (preset.filterBlocks || preset.filterEntities) {
                    HitResult hitResult = Minecraft.getInstance().hitResult;
                    if (!(preset.filterBlocks && hitResult instanceof BlockHitResult && hitResult.getType() == HitResult.Type.BLOCK)
                            && !(preset.filterEntities && hitResult instanceof EntityHitResult)) {
                        releaseClick(); // Unfulfilled filters release the click too.
                    }
                }
                break;
            case COOLDOWN_AWARE:
                boolean ready = Minecraft.getInstance().player != null &&
                        Minecraft.getInstance().player.getAttackStrengthScale(1.0F) >= 1.0F;
                if (ready && !clicking) {
                    startClick();
                    clickReleaseTime = now + 100 * 1_000_000L; // Max time until release
                }
                if ((ready || now >= clickReleaseTime) && clicking) {
                    releaseClick();
                }
        }
    }

    public ClickGuardClient.@Nullable AutoStoppedInfo checkActions() {
        // Check health and hunger actions:
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            for (ConfigManager.ConfigData.SliderAction action : preset.healthActions) {
                if (player.getHealth() <= action.points && !action.triggered) {
                    action.triggered = true;
                    if (triggerSliderAction(action, "clickguard.action.health.notification", (int) player.getHealth())) {
                        return new ClickGuardClient.AutoStoppedInfo(preset, Component.translatable("clickguard.action.health.hud", preset.name, action.points));
                    }
                } else if (player.getHealth() > action.points && action.triggered) {
                    action.triggered = false;
                    // Enable the action after going above the threshold.
                }
            }
            for (ConfigManager.ConfigData.SliderAction action : preset.hungerActions) {
                if (player.getFoodData().getFoodLevel() <= action.points && !action.triggered) {
                    action.triggered = true;
                    if (triggerSliderAction(action, "clickguard.action.hunger.notification", player.getFoodData().getFoodLevel())) {
                        return new ClickGuardClient.AutoStoppedInfo(preset, Component.translatable("clickguard.action.hunger.hud", preset.name, action.points));
                    }
                } else if (player.getFoodData().getFoodLevel() > action.points && action.triggered) {
                    action.triggered = false;
                    // Enable the action after going above the threshold.
                }
            }
            for (ConfigManager.ConfigData.FractionAction action : preset.durabilityActions) {
                ItemStack heldItem = player.getMainHandItem();
                if (heldItem.getMaxDamage() > 0) {
                    double durabilityPercent = 1d - ((double) heldItem.getDamageValue() / heldItem.getMaxDamage());
                    if (durabilityPercent <= action.fraction && !action.triggered) {
                        action.triggered = true;

                        if (action.notification) {
                            SystemNotifier.notify(Component.translatable("clickguard.action.durability.notification.title", String.format("%.2f", durabilityPercent * 100)).getString(),
                                    Component.translatable("clickguard.action.durability.notification.message", String.format("%.2f", action.fraction * 100)).getString());
                        }
                        if (Minecraft.getInstance().level != null && ClickGuardClient.pendingDisconnect == null && action.leaveWorld) {
                            ClickGuardClient.pendingDisconnect = new ClickGuardClient.AutoDisconnectInfo(preset, action);
                        }

                        if (action.stopClicker) {
                            return new ClickGuardClient.AutoStoppedInfo(preset, Component.translatable("clickguard.action.durability.hud", preset.name, String.format("%.2f", action.fraction * 100)));
                        }
                    } else if (durabilityPercent > action.fraction && action.triggered) {
                        action.triggered = false;
                        // Enable the action after going above the threshold.
                    }
                } else {
                    action.triggered = false;
                    // Enable the action after holding an item without durability or nothing
                }
            }
        }
        for (ConfigManager.ConfigData.TimeAction action : preset.waitTimeActions) {
            if (lastClickTime + action.timeMS * 1_000_000L <= System.nanoTime() && !action.triggered) {
                action.triggered = true;

                if (action.notification) {
                    SystemNotifier.notify(Component.translatable("clickguard.action.waitTime.notification.title", formatDuration((System.nanoTime() - lastClickTime) / 1_000_000L)).getString(),
                            Component.translatable("clickguard.action.waitTime.notification.message", formatDuration(action.timeMS)).getString());
                }
                if (Minecraft.getInstance().level != null && ClickGuardClient.pendingDisconnect == null && action.leaveWorld) {
                    ClickGuardClient.pendingDisconnect = new ClickGuardClient.AutoDisconnectInfo(preset, action);
                }
                if (action.stopClicker) {
                    return new ClickGuardClient.AutoStoppedInfo(preset, Component.translatable("clickguard.action.waitTime.hud", preset.name, formatDuration(action.timeMS)));
                }
            } else if (lastClickTime + action.timeMS * 1_000_000L > System.nanoTime() && action.triggered) {
                action.triggered = false;
                // Enable the action after going above the threshold.
            }
        }
        return null;
    }

    private void startClick() {
        // Check filters:
        if (preset.filterBlocks || preset.filterEntities) {
            HitResult hitResult = Minecraft.getInstance().hitResult;
            if (!(preset.filterBlocks && hitResult instanceof BlockHitResult && hitResult.getType() == HitResult.Type.BLOCK)
                    && !(preset.filterEntities && hitResult instanceof EntityHitResult)) {
                return;
            }
        }
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
        lastClickTime = System.nanoTime();
        preset.keybind.setDown(false);
        clicking = false;
    }

    boolean triggerSliderAction(ConfigManager.ConfigData.SliderAction action, final String notificationBaseKey, int pointsLeft) { // notificationBaseKey: e.g. "clickguard.action.health.notification"
        if (action.notification) {
            SystemNotifier.notify(Component.translatable(notificationBaseKey + ".title", pointsLeft).getString(),
                    Component.translatable(notificationBaseKey + ".message", action.points).getString());
        }
        if (Minecraft.getInstance().level != null && ClickGuardClient.pendingDisconnect == null && action.leaveWorld) {
            ClickGuardClient.pendingDisconnect = new ClickGuardClient.AutoDisconnectInfo(preset, action);
        }
        return action.stopClicker;
    }
}