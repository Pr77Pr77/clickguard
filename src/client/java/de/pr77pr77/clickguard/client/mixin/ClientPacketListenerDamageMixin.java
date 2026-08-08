package de.pr77pr77.clickguard.client.mixin;

import de.pr77pr77.clickguard.client.Clicker;
import de.pr77pr77.clickguard.client.SystemNotifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
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
        if(Minecraft.getInstance().level == null){
            return;
        }

        Entity entity = Minecraft.getInstance().level.getEntity(packet.entityId());

        if (autoClickingEnabled && entity == Minecraft.getInstance().player) {
            Minecraft.getInstance().execute(() -> {
                boolean notificationSent = false; // Prevent sending notifications multiple times
                boolean clickerDisablingScheduled = false; // Prevent sending notifications multiple times
                for (Clicker clicker : clickers) {
                    if (!clickerDisablingScheduled && clicker.preset.playerDamaged.stopClicker) {
                        clickerDisablingScheduled = true;
                    }
                    if (!notificationSent && clicker.preset.playerDamaged.notification) {
                        SystemNotifier.notify(Component.translatable("clickguard.filter.action.playerDamaged.notfication.title").getString(),
                                Component.translatable("clickguard.filter.action.playerDamaged.notfication.message").getString());
                        notificationSent = true;
                    }
                    if (Minecraft.getInstance().level != null && clicker.preset.playerDamaged.leaveWorld) {
                        Minecraft.getInstance().disconnect(new TitleScreen(), false);
                    }

                    if (clickerDisablingScheduled && notificationSent && Minecraft.getInstance().level == null) {
                        break; // No need to check the other clickers, everything is already done.
                    }
                }
                if (clickerDisablingScheduled && autoClickingEnabled) {
                    toggleAutoClickingEnabled();
                }
            });
        }
    }
}