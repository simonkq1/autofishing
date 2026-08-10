package xyz.whatsyouss.frostyautofish.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.whatsyouss.frostyautofish.BackgroundRunState;

@Mixin(Minecraft.class)
abstract class MinecraftPauseMixin {
    @Inject(method = "pauseIfInactive", at = @At("HEAD"), cancellable = true)
    private void frostyAutoFish$preventInactivePause(CallbackInfo callbackInfo) {
        if (BackgroundRunState.isActive()) {
            callbackInfo.cancel();
        }
    }
}
