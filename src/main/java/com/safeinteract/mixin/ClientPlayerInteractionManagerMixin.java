package com.safeinteract.mixin;

import com.safeinteract.handler.InteractionStateHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class ClientPlayerInteractionManagerMixin {
    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void safeinteract$guard(LocalPlayer player, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        if (InteractionStateHandler.shouldCancelInteractBlock(player, hand, hit)) cir.setReturnValue(InteractionResult.FAIL);
    }

    @Inject(method = "useItemOn", at = @At("RETURN"))
    private void safeinteract$afterInteract(LocalPlayer player, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        InteractionStateHandler.afterInteractBlock(cir.getReturnValue());
    }

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void safeinteract$guardEntityAtLocation(Player player, Entity entity, EntityHitResult hit, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (InteractionStateHandler.shouldCancelInteractEntity(player, entity, hand)) cir.setReturnValue(InteractionResult.CONSUME);
    }

    @Inject(method = "startDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void safeinteract$guardAttackBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (InteractionStateHandler.shouldCancelAttack(Minecraft.getInstance().player)) cir.setReturnValue(false);
    }

    @Inject(method = "continueDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void safeinteract$guardBlockBreaking(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (InteractionStateHandler.shouldCancelAttack(Minecraft.getInstance().player)) cir.setReturnValue(false);
    }

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void safeinteract$guardAttackEntity(Player player, Entity target, CallbackInfo ci) {
        if (InteractionStateHandler.shouldCancelAttack(player)) ci.cancel();
    }
}
