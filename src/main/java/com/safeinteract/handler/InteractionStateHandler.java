package com.safeinteract.handler;

import com.safeinteract.SafeInteractClient;
import com.safeinteract.config.ModConfig;
import com.safeinteract.util.Feedback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.InputUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.AbstractBoatEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.hit.BlockHitResult;

public final class InteractionStateHandler {
    private static int holdTicks;
    private static boolean startedOnBlockPlace;
    private static boolean pendingPlacementCheck;
    private static long lastGuardBlockMs;
    private static long lastGuardSoundMs;
    private static long lastDurabilityFeedbackMs;

    private InteractionStateHandler() {}

    public static void onPhysicalKeyReleased(InputUtil.Key key) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.options != null && key.equals(KeyBindingHelper.getBoundKeyOf(client.options.useKey))) {
            holdTicks = 0;
            startedOnBlockPlace = false;
        }
    }

    public static void tick(MinecraftClient client) {
        if (client.options.useKey.isPressed()) holdTicks++;
        else {
            holdTicks = 0;
            startedOnBlockPlace = false;
        }
    }

    public static boolean shouldCancelInteractBlock(ClientPlayerEntity player, Hand hand, BlockHitResult hit) {
        pendingPlacementCheck = false;
        MinecraftClient client = MinecraftClient.getInstance();
        ModConfig config = ModConfig.get();
        if (!config.enabled || client.world == null) return false;
        BlockState state = client.world.getBlockState(hit.getBlockPos());
        ItemStack stack = player.getStackInHand(hand);
        boolean interactive = isInteractiveBlock(state);
        boolean sneaking = player.isSneaking();
        long now = Util.getMeasuringTimeMs();
        if (config.buildGuardEnabled && interactive && !sneaking
                && (holdTicks > config.holdThresholdTicks || startedOnBlockPlace)
                && !AutoHoldHandler.isUseTarget(hit.getBlockPos())) {
            guardFeedback(now);
            return true;
        }
        if (config.durabilityGuardEnabled && config.durabilityGuardManualTools && isGuardedTool(stack)
                && AutoHoldHandler.isLowDurability(stack, config.durabilityThreshold) && (!interactive || sneaking)) {
            protectTool(stack, now);
            return true;
        }
        pendingPlacementCheck = stack.getItem() instanceof BlockItem && (!interactive || sneaking);
        return false;
    }

    public static boolean isEntityGuarded(Entity entity) {
        return entity instanceof ItemFrameEntity || entity instanceof ArmorStandEntity || entity instanceof MerchantEntity
                || entity instanceof AbstractMinecartEntity || entity instanceof AbstractBoatEntity;
    }

    public static boolean shouldCancelInteractEntity(PlayerEntity player, Entity entity, Hand hand) {
        ModConfig config = ModConfig.get();
        if (!config.enabled) return false;
        long now = Util.getMeasuringTimeMs();
        if (config.buildGuardEnabled && config.entityGuardEnabled && isEntityGuarded(entity) && !player.isSneaking()
                && (holdTicks > config.holdThresholdTicks || startedOnBlockPlace) && !AutoHoldHandler.isUseTarget(entity)) {
            guardFeedback(now);
            return true;
        }
        ItemStack stack = player.getStackInHand(hand);
        if (config.durabilityGuardEnabled && config.durabilityGuardManualTools && isGuardedTool(stack)
                && AutoHoldHandler.isLowDurability(stack, config.durabilityThreshold)) {
            protectTool(stack, now);
            return true;
        }
        return false;
    }

    public static boolean shouldCancelAttack(PlayerEntity player) {
        if (player == null) return false;
        ModConfig config = ModConfig.get();
        ItemStack stack = player.getMainHandStack();
        if (config.enabled && config.durabilityGuardEnabled && config.durabilityGuardMining && isMiningGuarded(stack)
                && AutoHoldHandler.isLowDurability(stack, config.durabilityThreshold)) {
            protectTool(stack, Util.getMeasuringTimeMs());
            return true;
        }
        return false;
    }

    private static void guardFeedback(long now) {
        lastGuardBlockMs = now;
        if (lastGuardSoundMs == 0 || now - lastGuardSoundMs >= 600) {
            lastGuardSoundMs = now;
            Feedback.sound(SoundEvents.BLOCK_NOTE_BLOCK_HAT, 1.5f);
        }
    }

    private static void protectTool(ItemStack stack, long now) {
        lastGuardBlockMs = now;
        if (lastDurabilityFeedbackMs == 0 || now - lastDurabilityFeedbackMs >= 1000) {
            lastDurabilityFeedbackMs = now;
            Feedback.actionBar(Text.translatable("message.safeinteract.tool_protected", stack.getMaxDamage() - stack.getDamage()));
            Feedback.sound(SoundEvents.BLOCK_NOTE_BLOCK_HAT, 0.8f);
        }
    }

    public static void afterInteractBlock(ActionResult result) {
        if (pendingPlacementCheck && result instanceof ActionResult.Success) startedOnBlockPlace = true;
        pendingPlacementCheck = false;
    }

    private static boolean isGuardedTool(ItemStack stack) {
        Item item = stack.getItem();
        return item instanceof AxeItem || item instanceof HoeItem || item instanceof ShovelItem
                || item instanceof FlintAndSteelItem || item instanceof ShearsItem || item instanceof BrushItem;
    }

    private static boolean isMiningGuarded(ItemStack stack) {
        Item item = stack.getItem();
        return stack.contains(DataComponentTypes.TOOL) || item instanceof SwordItem
                || item instanceof MaceItem || item instanceof TridentItem;
    }

    public static boolean isInteractiveBlock(BlockState state) {
        ModConfig config = ModConfig.get();
        Identifier id = Registries.BLOCK.getId(state.getBlock());
        if (config.isIgnored(id)) return false;
        if (config.isGuarded(id)) return true;
        Block block = state.getBlock();
        if (block instanceof BlockWithEntity && !(block instanceof AbstractSkullBlock)
                && !(block instanceof AbstractBannerBlock) && !(block instanceof SculkSensorBlock)
                && !(block instanceof SculkShriekerBlock) && !(block instanceof SculkCatalystBlock)
                && !(block instanceof ConduitBlock) && !(block instanceof PistonExtensionBlock)
                && !(block instanceof BrushableBlock) && !(block instanceof CreakingHeartBlock)
                && !(block instanceof EndPortalBlock) && !(block instanceof EndGatewayBlock)) return true;
        return block instanceof DoorBlock || block instanceof TrapdoorBlock || block instanceof FenceGateBlock
                || block instanceof ButtonBlock || block instanceof LeverBlock || block instanceof AbstractRedstoneGateBlock
                || block instanceof NoteBlock || block instanceof BedBlock
                || (block instanceof CraftingTableBlock && !(block instanceof FletchingTableBlock))
                || block instanceof AnvilBlock || block instanceof GrindstoneBlock || block instanceof StonecutterBlock
                || block instanceof LoomBlock || block instanceof CartographyTableBlock || block instanceof SmithingTableBlock
                || block instanceof EnchantingTableBlock || block instanceof BeaconBlock || block instanceof BellBlock
                || block instanceof ComposterBlock || block instanceof LecternBlock || block instanceof RespawnAnchorBlock
                || block instanceof ChiseledBookshelfBlock || block instanceof DecoratedPotBlock;
    }

    public static boolean isPierceActive() {
        ModConfig config = ModConfig.get();
        return config.enabled && config.entityPierceEnabled && SafeInteractClient.PIERCE_KEY != null
                && SafeInteractClient.PIERCE_KEY.isPressed();
    }

    public static boolean isPierceable(Entity entity) {
        ModConfig c = ModConfig.get();
        return entity instanceof ItemFrameEntity && c.pierceItemFrames
                || entity instanceof ArmorStandEntity && c.pierceArmorStands
                || entity instanceof MerchantEntity && c.pierceVillagers
                || entity instanceof AbstractMinecartEntity && c.pierceMinecarts
                || entity instanceof AbstractBoatEntity && c.pierceBoats
                || entity instanceof PaintingEntity && c.piercePaintings;
    }

    public static void reset() {
        holdTicks = 0;
        startedOnBlockPlace = false;
        pendingPlacementCheck = false;
        lastGuardBlockMs = 0;
        lastGuardSoundMs = 0;
        lastDurabilityFeedbackMs = 0;
    }

    public static long getLastGuardBlockMs() { return lastGuardBlockMs; }
}
