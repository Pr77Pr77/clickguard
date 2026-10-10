package de.pr77pr77.clickguard.client.mixin;

import de.pr77pr77.clickguard.client.Clicker;
import de.pr77pr77.clickguard.client.HUD;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;
import java.util.function.Consumer;

import static de.pr77pr77.clickguard.client.ClickGuardClient.*;

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
            if (!autoClickingEnabled) {
                operation.accept(keyMapping);
                return;
            }
            Optional<Clicker> matchingClicker = clickers.stream().filter(clicker -> clicker.preset.keybind == keyMappingObj).findFirst();
            if (matchingClicker.isEmpty()) {
                operation.accept(keyMapping);
            } else {
                if (configManager.data.allowClickingWhenEnabled && !matchingClicker.get().getClicking()) {
                    operation.accept(keyMapping);
                } else if (HUD.warningStartTime < 0 && !configManager.data.allowClickingWhenEnabled) {
                    Minecraft.getInstance().getSoundManager().play(
                            SimpleSoundInstance.forUI(SoundEvents.VILLAGER_NO, 1.0F, 1.0F)
                    );
                    HUD.triggerWarning();
                }
            }
        }
    }
}
