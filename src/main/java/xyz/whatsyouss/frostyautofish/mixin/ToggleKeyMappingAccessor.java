package xyz.whatsyouss.frostyautofish.mixin;

import net.minecraft.client.ToggleKeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ToggleKeyMapping.class)
public interface ToggleKeyMappingAccessor {
    @Accessor("releasedByScreenWhenDown")
    void frostyAutoFish$setReleasedByScreenWhenDown(boolean value);
}
