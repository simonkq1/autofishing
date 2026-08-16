package xyz.whatsyouss.frostyautofish.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.whatsyouss.frostyautofish.FrostyAutoFishClient;
import xyz.whatsyouss.frostyautofish.HighValueTargetSnapshot;

import java.util.Locale;

@Mixin(Gui.class)
public abstract class GuiMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void frostyAutoFish$extractHighValueHud(
            GuiGraphicsExtractor graphics,
            DeltaTracker deltaTracker,
            CallbackInfo callback
    ) {
        HighValueTargetSnapshot snapshot = FrostyAutoFishClient.highValueHudSnapshot();
        if (snapshot == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        String attacks = snapshot.autoAttackEnabled()
                ? "hits " + snapshot.attacksDone() + "/" + snapshot.attackLimit()
                : "attack off";
        String text = "High Value: " + snapshot.name()
                + " | " + String.format(Locale.ROOT, "%.1fm", snapshot.distance())
                + " | " + attacks;
        int x = 8;
        int y = 8;
        int width = font.width(text) + 8;
        graphics.fill(x - 4, y - 3, x + width, y + 12, 0x88000000);
        graphics.text(font, Component.literal(text), x, y, 0xFFFFAA00);
    }
}
