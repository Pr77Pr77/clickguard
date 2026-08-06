package de.pr77pr77.clickguard.client.mixin;

import net.minecraft.client.KeyMapping;
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
        if(keyMappingObj instanceof KeyMapping keyMapping) {
            if(!autoClickingEnabled ||
                    clickers.stream().noneMatch(clicker -> clicker.preset.keybind == keyMappingObj)){
                operation.accept(keyMapping);
            } else {
                // TODO: Trigger HUD Warning
            }
        }
    }
}
