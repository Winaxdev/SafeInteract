package com.safeinteract.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import com.safeinteract.handler.InteractionStateHandler;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyMapping.class)
public abstract class KeyBindingMixin {
    @Inject(method = "set", at = @At("HEAD"))
    private static void safeinteract$physicalKey(InputConstants.Key key, boolean pressed, CallbackInfo ci) {
        if (!pressed) InteractionStateHandler.onPhysicalKeyReleased(key);
    }
}
