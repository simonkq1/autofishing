package xyz.whatsyouss.frostyautofish.mixin;

import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.util.SmoothDouble;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MouseHandler.class)
public interface MouseHandlerAccessor {
    @Accessor("smoothTurnX")
    SmoothDouble frostyAutoFish$getSmoothTurnX();

    @Accessor("smoothTurnY")
    SmoothDouble frostyAutoFish$getSmoothTurnY();

    @Accessor("accumulatedDX")
    void frostyAutoFish$setAccumulatedDX(double value);

    @Accessor("accumulatedDY")
    void frostyAutoFish$setAccumulatedDY(double value);

    @Accessor("isLeftPressed")
    void frostyAutoFish$setLeftPressed(boolean value);

    @Accessor("isMiddlePressed")
    void frostyAutoFish$setMiddlePressed(boolean value);

    @Accessor("isRightPressed")
    void frostyAutoFish$setRightPressed(boolean value);

    @Accessor("activeButton")
    void frostyAutoFish$setActiveButton(MouseButtonInfo value);

    @Accessor("fakeRightMouse")
    int frostyAutoFish$getFakeRightMouse();

    @Accessor("fakeRightMouse")
    void frostyAutoFish$setFakeRightMouse(int value);

    @Accessor("clickDepth")
    void frostyAutoFish$setClickDepth(int value);

    @Accessor("mousePressedTime")
    void frostyAutoFish$setMousePressedTime(double value);
}
