package com.safeinteract.handler;

import com.mojang.blaze3d.platform.InputConstants;
import com.safeinteract.SafeInteractClient;
import com.safeinteract.config.ModConfig;
import com.safeinteract.util.Feedback;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BrushItem;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.block.AbstractBannerBlock;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.BeaconBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BrushableBlock;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CartographyTableBlock;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.ConduitBlock;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.CreakingHeartBlock;
import net.minecraft.world.level.block.DecoratedPotBlock;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.EndGatewayBlock;
import net.minecraft.world.level.block.EndPortalBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.GrindstoneBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.LoomBlock;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.SculkCatalystBlock;
import net.minecraft.world.level.block.SculkSensorBlock;
import net.minecraft.world.level.block.SculkShriekerBlock;
import net.minecraft.world.level.block.SmithingTableBlock;
import net.minecraft.world.level.block.StonecutterBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.piston.MovingPistonBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class InteractionStateHandler {
    private static int holdTicks;
    private static boolean startedOnBlockPlace;
    private static boolean pendingPlacementCheck;
    private static long lastGuardBlockMs;
    private static long lastGuardSoundMs;
    private static long lastDurabilityFeedbackMs;

    private InteractionStateHandler() {}

    public static void onPhysicalKeyReleased(InputConstants.Key key) {
        Minecraft client = Minecraft.getInstance();
        if (client != null && client.options != null && key.equals(KeyMappingHelper.getBoundKeyOf(client.options.keyUse))) {
            holdTicks = 0;
            startedOnBlockPlace = false;
        }
    }

    public static void tick(Minecraft client) {
        if (client.options.keyUse.isDown()) holdTicks++;
        else {
            holdTicks = 0;
            startedOnBlockPlace = false;
        }
    }

    public static boolean shouldCancelInteractBlock(LocalPlayer player, InteractionHand hand, BlockHitResult hit) {
        pendingPlacementCheck = false;
        Minecraft client = Minecraft.getInstance();
        ModConfig config = ModConfig.get();
        if (!config.enabled || client.level == null) return false;
        BlockState state = client.level.getBlockState(hit.getBlockPos());
        ItemStack stack = player.getItemInHand(hand);
        boolean interactive = isInteractiveBlock(state);
        boolean sneaking = player.isShiftKeyDown();
        long now = Util.getMillis();
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
        return entity instanceof ItemFrame || entity instanceof ArmorStand || entity instanceof AbstractVillager
                || entity instanceof AbstractMinecart || entity instanceof AbstractBoat;
    }

    public static boolean shouldCancelInteractEntity(Player player, Entity entity, InteractionHand hand) {
        ModConfig config = ModConfig.get();
        if (!config.enabled) return false;
        long now = Util.getMillis();
        if (config.buildGuardEnabled && config.entityGuardEnabled && isEntityGuarded(entity) && !player.isShiftKeyDown()
                && (holdTicks > config.holdThresholdTicks || startedOnBlockPlace) && !AutoHoldHandler.isUseTarget(entity)) {
            guardFeedback(now);
            return true;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (config.durabilityGuardEnabled && config.durabilityGuardManualTools && isGuardedTool(stack)
                && AutoHoldHandler.isLowDurability(stack, config.durabilityThreshold)) {
            protectTool(stack, now);
            return true;
        }
        return false;
    }

    public static boolean shouldCancelAttack(Player player) {
        if (player == null) return false;
        ModConfig config = ModConfig.get();
        ItemStack stack = player.getMainHandItem();
        if (config.enabled && config.durabilityGuardEnabled && config.durabilityGuardMining && isMiningGuarded(stack)
                && AutoHoldHandler.isLowDurability(stack, config.durabilityThreshold)) {
            protectTool(stack, Util.getMillis());
            return true;
        }
        return false;
    }

    private static void guardFeedback(long now) {
        lastGuardBlockMs = now;
        if (lastGuardSoundMs == 0 || now - lastGuardSoundMs >= 600) {
            lastGuardSoundMs = now;
            Feedback.sound(SoundEvents.NOTE_BLOCK_HAT, 1.5f);
        }
    }

    private static void protectTool(ItemStack stack, long now) {
        lastGuardBlockMs = now;
        if (lastDurabilityFeedbackMs == 0 || now - lastDurabilityFeedbackMs >= 1000) {
            lastDurabilityFeedbackMs = now;
            Feedback.actionBar(Component.translatable("message.safeinteract.tool_protected", stack.getMaxDamage() - stack.getDamageValue()));
            Feedback.sound(SoundEvents.NOTE_BLOCK_HAT, 0.8f);
        }
    }

    public static void afterInteractBlock(InteractionResult result) {
        if (pendingPlacementCheck && result instanceof InteractionResult.Success) startedOnBlockPlace = true;
        pendingPlacementCheck = false;
    }

    private static boolean isGuardedTool(ItemStack stack) {
        Item item = stack.getItem();
        return stack.has(DataComponents.BLOCK_TRANSFORMER)
                || item instanceof FlintAndSteelItem || item instanceof ShearsItem || item instanceof BrushItem;
    }

    private static boolean isMiningGuarded(ItemStack stack) {
        Item item = stack.getItem();
        return stack.has(DataComponents.TOOL) || stack.has(DataComponents.WEAPON)
                || item instanceof MaceItem || item instanceof TridentItem;
    }

    public static boolean isInteractiveBlock(BlockState state) {
        ModConfig config = ModConfig.get();
        Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (config.isIgnored(id)) return false;
        if (config.isGuarded(id)) return true;
        Block block = state.getBlock();
        if (block instanceof BaseEntityBlock && !(block instanceof AbstractSkullBlock)
                && !(block instanceof AbstractBannerBlock) && !(block instanceof SculkSensorBlock)
                && !(block instanceof SculkShriekerBlock) && !(block instanceof SculkCatalystBlock)
                && !(block instanceof ConduitBlock) && !(block instanceof MovingPistonBlock)
                && !(block instanceof BrushableBlock) && !(block instanceof CreakingHeartBlock)
                && !(block instanceof EndPortalBlock) && !(block instanceof EndGatewayBlock)) return true;
        return block instanceof DoorBlock || block instanceof TrapDoorBlock || block instanceof FenceGateBlock
                || block instanceof ButtonBlock || block instanceof LeverBlock || block instanceof DiodeBlock
                || block instanceof NoteBlock || block instanceof BedBlock
                || block instanceof CraftingTableBlock
                || block instanceof AnvilBlock || block instanceof GrindstoneBlock || block instanceof StonecutterBlock
                || block instanceof LoomBlock || block instanceof CartographyTableBlock || block instanceof SmithingTableBlock
                || block instanceof EnchantingTableBlock || block instanceof BeaconBlock || block instanceof BellBlock
                || block instanceof ComposterBlock || block instanceof LecternBlock || block instanceof RespawnAnchorBlock
                || block instanceof ChiseledBookShelfBlock || block instanceof DecoratedPotBlock;
    }

    public static boolean isPierceActive() {
        ModConfig config = ModConfig.get();
        return config.enabled && config.entityPierceEnabled && SafeInteractClient.PIERCE_KEY != null
                && SafeInteractClient.PIERCE_KEY.isDown() && !ServerPolicy.isPierceBlocked();
    }

    public static boolean isPierceable(Entity entity) {
        ModConfig c = ModConfig.get();
        return entity instanceof ItemFrame && c.pierceItemFrames
                || entity instanceof ArmorStand && c.pierceArmorStands
                || entity instanceof AbstractVillager && c.pierceVillagers
                || entity instanceof AbstractMinecart && c.pierceMinecarts
                || entity instanceof AbstractBoat && c.pierceBoats
                || entity instanceof Painting && c.piercePaintings;
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
