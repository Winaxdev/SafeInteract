package com.safeinteract.mixin;

import com.safeinteract.render.CrosshairHudRenderer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public abstract class InGameHudMixin {
    @Inject(method = "extractCrosshair", at = @At("TAIL"))
    private void safeinteract$renderCrosshair(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
        CrosshairHudRenderer.render(context, tickCounter);
    }
}
