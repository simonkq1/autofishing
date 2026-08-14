package xyz.whatsyouss.frostyautofish.mixin;

import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(KeyMapping.class)
public interface KeyMappingAccessor {
    @Accessor("isDown")
    void frostyAutoFish$setDownExactly(boolean value);
}
