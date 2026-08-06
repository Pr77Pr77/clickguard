package de.pr77pr77.clickguard.client.mixin;

import de.pr77pr77.clickguard.client.HUD;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Consumer;

import static de.pr77pr77.clickguard.client.ClickGuardClient.autoClickingEnabled;
import static de.pr77pr77.clickguard.client.ClickGuardClient.clickers;

@Mixin(KeyMapping.class)
public class ManualClicksBlockingMixin {
    @Redirect(
            method = "forAllKeyMappings(Lcom/mojang/blaze3d/platform/InputConstants$Key;Ljava/util/function/Consumer;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V"
            )
    )
    private static void clickguard$onOperationAccept(Consumer<KeyMapping> operation, Object keyMappingObj) {
        if (keyMappingObj instanceof KeyMapping keyMapping) {
            if (!autoClickingEnabled ||
                    clickers.stream().noneMatch(clicker -> clicker.preset.keybind == keyMappingObj)) {
                operation.accept(keyMapping);
            } else {
                if (HUD.warningStartTime < 0) {
                    Minecraft.getInstance().getSoundManager().play(
                            SimpleSoundInstance.forUI(SoundEvents.VILLAGER_NO, 1.0F)
                    );
                    HUD.triggerWarning();
                }
            }
        }
    }
}
