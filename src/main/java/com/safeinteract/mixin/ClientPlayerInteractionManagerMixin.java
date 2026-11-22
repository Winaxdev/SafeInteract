package com.safeinteract.mixin;

import com.safeinteract.handler.InteractionStateHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class ClientPlayerInteractionManagerMixin {
    @Inject(method = "interactBlock", at = @At("HEAD"), cancellable = true)
    private void safeinteract$guard(ClientPlayerEntity player, Hand hand, BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir) {
        if (InteractionStateHandler.shouldCancelInteractBlock(player, hand, hit)) cir.setReturnValue(ActionResult.FAIL);
    }

    @Inject(method = "interactBlock", at = @At("RETURN"))
    private void safeinteract$afterInteract(ClientPlayerEntity player, Hand hand, BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir) {
        InteractionStateHandler.afterInteractBlock(cir.getReturnValue());
    }

    @Inject(method = "interactEntity", at = @At("HEAD"), cancellable = true)
    private void safeinteract$guardEntity(PlayerEntity player, Entity entity, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (InteractionStateHandler.shouldCancelInteractEntity(player, entity, hand)) cir.setReturnValue(ActionResult.CONSUME);
    }

    @Inject(method = "interactEntityAtLocation", at = @At("HEAD"), cancellable = true)
    private void safeinteract$guardEntityAtLocation(PlayerEntity player, Entity entity, EntityHitResult hit, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (InteractionStateHandler.shouldCancelInteractEntity(player, entity, hand)) cir.setReturnValue(ActionResult.CONSUME);
    }

    @Inject(method = "attackBlock", at = @At("HEAD"), cancellable = true)
    private void safeinteract$guardAttackBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (InteractionStateHandler.shouldCancelAttack(MinecraftClient.getInstance().player)) cir.setReturnValue(false);
    }

    @Inject(method = "updateBlockBreakingProgress", at = @At("HEAD"), cancellable = true)
    private void safeinteract$guardBlockBreaking(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (InteractionStateHandler.shouldCancelAttack(MinecraftClient.getInstance().player)) cir.setReturnValue(false);
    }

    @Inject(method = "attackEntity", at = @At("HEAD"), cancellable = true)
    private void safeinteract$guardAttackEntity(PlayerEntity player, Entity target, CallbackInfo ci) {
        if (InteractionStateHandler.shouldCancelAttack(player)) ci.cancel();
    }
}
