package xyz.whatsyouss.frostyautofish.mixin;

import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.InputQuirks;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.whatsyouss.frostyautofish.InputLockCoordinator;

import static org.lwjgl.glfw.GLFW.GLFW_MOD_CONTROL;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Inject(method = "turnPlayer(D)V", at = @At("HEAD"), cancellable = true)
    private void frostyAutoFish$lockTurn(double frameTime, CallbackInfo callback) {
        if (InputLockCoordinator.blockMouseTurn()) {
            callback.cancel();
        }
    }

    @Inject(method = "onScroll(JDD)V", at = @At("HEAD"), cancellable = true)
    private void frostyAutoFish$lockScroll(
            long window,
            double horizontal,
            double vertical,
            CallbackInfo callback
    ) {
        if (InputLockCoordinator.blockWorldScroll()) {
            callback.cancel();
        }
    }

    @Inject(
            method = "onButton(JLnet/minecraft/client/input/MouseButtonInfo;I)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void frostyAutoFish$beforeButton(
            long window,
            MouseButtonInfo buttonInfo,
            int action,
            CallbackInfo callback
    ) {
        MouseButtonInfo effectiveButton = frostyAutoFish$effectiveButton(buttonInfo, action);
        if (InputLockCoordinator.beginMouseButtonEvent(window, effectiveButton, action)) {
            InputLockCoordinator.endMouseButtonEvent();
            callback.cancel();
        }
    }

    @Inject(
            method = "onButton(JLnet/minecraft/client/input/MouseButtonInfo;I)V",
            at = @At("RETURN")
    )
    private void frostyAutoFish$afterButton(
            long window,
            MouseButtonInfo buttonInfo,
            int action,
            CallbackInfo callback
    ) {
        InputLockCoordinator.endMouseButtonEvent();
    }

    private MouseButtonInfo frostyAutoFish$effectiveButton(MouseButtonInfo buttonInfo, int action) {
        if (!InputQuirks.SIMULATE_RIGHT_CLICK_WITH_LONG_LEFT_CLICK
                || buttonInfo.button() != GLFW_MOUSE_BUTTON_LEFT) {
            return buttonInfo;
        }
        boolean simulatedPress = action == GLFW_PRESS
                && (buttonInfo.modifiers() & GLFW_MOD_CONTROL) == GLFW_MOD_CONTROL;
        boolean simulatedRelease = action != GLFW_PRESS
                && ((MouseHandlerAccessor) this).frostyAutoFish$getFakeRightMouse() > 0;
        if (!simulatedPress && !simulatedRelease) {
            return buttonInfo;
        }
        return new MouseButtonInfo(GLFW_MOUSE_BUTTON_RIGHT, buttonInfo.modifiers());
    }
}
