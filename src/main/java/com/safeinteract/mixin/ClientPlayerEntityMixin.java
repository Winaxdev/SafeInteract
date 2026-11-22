package com.safeinteract.mixin;

import com.safeinteract.handler.InteractionStateHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.function.Predicate;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin {
    @ModifyArg(method = "getCrosshairTarget(Lnet/minecraft/entity/Entity;DDF)Lnet/minecraft/util/hit/HitResult;", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/projectile/ProjectileUtil;raycast(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Box;Ljava/util/function/Predicate;D)Lnet/minecraft/util/hit/EntityHitResult;"), index = 4)
    private static Predicate<Entity> safeinteract$pierce(Predicate<Entity> original) {
        return InteractionStateHandler.isPierceActive()
                ? original.and(entity -> !InteractionStateHandler.isPierceable(entity)) : original;
    }

    @ModifyArg(method = "getCrosshairTarget(FLnet/minecraft/entity/Entity;)Lnet/minecraft/util/hit/HitResult;", at = @At(value = "INVOKE", target = "Lnet/minecraft/component/type/AttackRangeComponent;getHitResult(Lnet/minecraft/entity/Entity;FLjava/util/function/Predicate;)Lnet/minecraft/util/hit/HitResult;"), index = 2)
    private Predicate<Entity> safeinteract$pierceAttackRange(Predicate<Entity> original) {
        return InteractionStateHandler.isPierceActive()
                ? original.and(entity -> !InteractionStateHandler.isPierceable(entity)) : original;
    }
}
