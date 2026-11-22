package com.safeinteract.mixin;

import com.safeinteract.SafeInteractClient;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @Inject(method = "handleInputEvents", at = @At("HEAD"))
    private void safeinteract$preInput(CallbackInfo ci) {
        SafeInteractClient.onPreInput((MinecraftClient) (Object) this);
    }
}
