package de.pr77pr77.clickguard.client.mixin;

import de.pr77pr77.clickguard.client.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static de.pr77pr77.clickguard.client.ClickGuardClient.*;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerDamageMixin {

    @Inject(method = "handleDamageEvent(Lnet/minecraft/network/protocol/game/ClientboundDamageEventPacket;)V", at = @At("HEAD"))
    private void clickguard$onDamageEvent(ClientboundDamageEventPacket packet, CallbackInfo ci) {
        if (Minecraft.getInstance().level == null) {
            return;
        }

        Entity entity = Minecraft.getInstance().level.getEntity(packet.entityId());

        if ((autoClickingEnabled || autoStoppedInfo != null) && entity == Minecraft.getInstance().player) {
            Minecraft.getInstance().execute(() -> {
                boolean notificationSent = false; // Prevent sending notifications multiple times
                for (Clicker clicker : clickers) {
                    if (clicker.preset.playerDamaged.triggered) {
                        return;
                    }
                    clicker.preset.playerDamaged.triggered = true;

                    if (!notificationSent && clicker.preset.playerDamaged.notification) {
                        SystemNotifier.notify(Component.translatable("clickguard.action.playerDamaged.notfication.title").getString(),
                                Component.translatable("clickguard.action.playerDamaged.notfication.message").getString());
                        notificationSent = true;
                    }
                    if (Minecraft.getInstance().level != null && clicker.preset.playerDamaged.leaveWorld) {
                        pendingDisconnect = new AutoDisconnectInfo(clicker.preset, clicker.preset.playerDamaged);
                    }
                    if (clicker.preset.playerDamaged.stopClicker && autoStoppedInfo == null) {
                        autoStoppedInfo = new ClickGuardClient.AutoStoppedInfo(clicker.preset, Component.translatable("clickguard.action.playerDamaged.hud", clicker.preset.name));
                        if (notificationSent && Minecraft.getInstance().level == null) {
                            break;
                        }
                    }
                }
                if (autoStoppedInfo != null && autoClickingEnabled) {
                    toggleAutoClickingEnabled();
                }
            });
        }
    }
}