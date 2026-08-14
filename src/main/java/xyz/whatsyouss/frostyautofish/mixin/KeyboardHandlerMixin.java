package xyz.whatsyouss.frostyautofish.mixin;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.whatsyouss.frostyautofish.InputLockCoordinator;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
    @Inject(
            method = "keyPress(JILnet/minecraft/client/input/KeyEvent;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void frostyAutoFish$beforeKeyPress(
            long window,
            int action,
            KeyEvent event,
            CallbackInfo callback
    ) {
        if (InputLockCoordinator.beginKeyboardEvent(event, action)) {
            callback.cancel();
        }
    }

    @Inject(
            method = "keyPress(JILnet/minecraft/client/input/KeyEvent;)V",
            at = @At("RETURN")
    )
    private void frostyAutoFish$afterKeyPress(
            long window,
            int action,
            KeyEvent event,
            CallbackInfo callback
    ) {
        InputLockCoordinator.endKeyboardEvent();
    }
}
