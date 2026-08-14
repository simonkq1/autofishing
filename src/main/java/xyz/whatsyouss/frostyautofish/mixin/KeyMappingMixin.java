package xyz.whatsyouss.frostyautofish.mixin;

import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.whatsyouss.frostyautofish.InputLockCoordinator;

@Mixin(KeyMapping.class)
public abstract class KeyMappingMixin {
    @Inject(method = "restoreToggleStatesOnScreenClosed()V", at = @At("HEAD"))
    private static void frostyAutoFish$filterToggleRestore(CallbackInfo callback) {
        InputLockCoordinator.beforeToggleRestore();
    }

    @Inject(method = "setAll()V", at = @At("HEAD"), cancellable = true)
    private static void frostyAutoFish$gateSetAll(CallbackInfo callback) {
        if (InputLockCoordinator.gateSetAll()) {
            callback.cancel();
        }
    }
}
