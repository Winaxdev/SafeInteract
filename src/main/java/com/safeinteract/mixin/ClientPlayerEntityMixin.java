package com.safeinteract.mixin;

import com.safeinteract.handler.InteractionStateHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.function.Predicate;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;

@Mixin(LocalPlayer.class)
public abstract class ClientPlayerEntityMixin {
    @ModifyArg(method = "pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/ProjectileUtil;getEntityHitResult(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;D)Lnet/minecraft/world/phys/EntityHitResult;"), index = 4)
    private static Predicate<Entity> safeinteract$pierce(Predicate<Entity> original) {
        return InteractionStateHandler.isPierceActive()
                ? original.and(entity -> !InteractionStateHandler.isPierceable(entity)) : original;
    }

    @ModifyArg(method = "raycastHitResult(FLnet/minecraft/world/entity/Entity;)Lnet/minecraft/world/phys/HitResult;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/component/AttackRange;getClosesetHit(Lnet/minecraft/world/entity/Entity;FLjava/util/function/Predicate;)Lnet/minecraft/world/phys/HitResult;"), index = 2)
    private Predicate<Entity> safeinteract$pierceAttackRange(Predicate<Entity> original) {
        return InteractionStateHandler.isPierceActive()
                ? original.and(entity -> !InteractionStateHandler.isPierceable(entity)) : original;
    }
}
