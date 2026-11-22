package com.safeinteract.mixin;

import com.safeinteract.handler.InteractionStateHandler;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyBinding.class)
public abstract class KeyBindingMixin {
    @Inject(method = "setKeyPressed", at = @At("HEAD"))
    private static void safeinteract$physicalKey(InputUtil.Key key, boolean pressed, CallbackInfo ci) {
        if (!pressed) InteractionStateHandler.onPhysicalKeyReleased(key);
    }
}
