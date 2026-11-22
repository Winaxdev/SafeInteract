package com.safeinteract.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.List;
import java.util.function.Consumer;

public final class ClothConfigScreen {
    private ClothConfigScreen() {}

    public static Screen create(Screen parent) {
        ModConfig c = ModConfig.get();
        ModConfig defaults = new ModConfig();
        ConfigBuilder builder = ConfigBuilder.create().setParentScreen(parent)
                .setTitle(Text.translatable("config.safeinteract.title")).setSavingRunnable(ModConfig::save);
        ConfigEntryBuilder entries = builder.entryBuilder();
        ConfigCategory general = category(builder, "general");
        bool(general, entries, "enabled", c.enabled, defaults.enabled, v -> c.enabled = v);
        bool(general, entries, "actionBarEnabled", c.actionBarEnabled, defaults.actionBarEnabled, v -> c.actionBarEnabled = v);

        ConfigCategory build = category(builder, "build_guard");
        bool(build, entries, "buildGuardEnabled", c.buildGuardEnabled, defaults.buildGuardEnabled, v -> c.buildGuardEnabled = v);
        bool(build, entries, "entityGuardEnabled", c.entityGuardEnabled, defaults.entityGuardEnabled, v -> c.entityGuardEnabled = v);
        integer(build, entries, "holdThresholdTicks", c.holdThresholdTicks, defaults.holdThresholdTicks, 1, 20, v -> c.holdThresholdTicks = v);
        strings(build, entries, "guardedBlocks", c.guardedBlocks, defaults.guardedBlocks, v -> c.guardedBlocks = v);
        strings(build, entries, "ignoredBlocks", c.ignoredBlocks, defaults.ignoredBlocks, v -> c.ignoredBlocks = v);

        ConfigCategory tool = category(builder, "tool_safety");
        bool(tool, entries, "durabilityGuardEnabled", c.durabilityGuardEnabled, defaults.durabilityGuardEnabled, v -> c.durabilityGuardEnabled = v);
        integer(tool, entries, "durabilityThreshold", c.durabilityThreshold, defaults.durabilityThreshold, 1, 50, v -> c.durabilityThreshold = v);
        bool(tool, entries, "durabilityGuardManualTools", c.durabilityGuardManualTools, defaults.durabilityGuardManualTools, v -> c.durabilityGuardManualTools = v);
        bool(tool, entries, "durabilityGuardMining", c.durabilityGuardMining, defaults.durabilityGuardMining, v -> c.durabilityGuardMining = v);

        ConfigCategory pierce = category(builder, "entity_pierce");
        bool(pierce, entries, "entityPierceEnabled", c.entityPierceEnabled, defaults.entityPierceEnabled, v -> c.entityPierceEnabled = v);
        bool(pierce, entries, "pierceItemFrames", c.pierceItemFrames, defaults.pierceItemFrames, v -> c.pierceItemFrames = v);
        bool(pierce, entries, "pierceArmorStands", c.pierceArmorStands, defaults.pierceArmorStands, v -> c.pierceArmorStands = v);
        bool(pierce, entries, "pierceVillagers", c.pierceVillagers, defaults.pierceVillagers, v -> c.pierceVillagers = v);
        bool(pierce, entries, "pierceMinecarts", c.pierceMinecarts, defaults.pierceMinecarts, v -> c.pierceMinecarts = v);
        bool(pierce, entries, "pierceBoats", c.pierceBoats, defaults.pierceBoats, v -> c.pierceBoats = v);
        bool(pierce, entries, "piercePaintings", c.piercePaintings, defaults.piercePaintings, v -> c.piercePaintings = v);

        ConfigCategory hold = category(builder, "smart_hold");
        bool(hold, entries, "holdFullInventoryGuardUse", c.holdFullInventoryGuardUse, defaults.holdFullInventoryGuardUse, v -> c.holdFullInventoryGuardUse = v);
        bool(hold, entries, "holdFullInventoryGuardAttack", c.holdFullInventoryGuardAttack, defaults.holdFullInventoryGuardAttack, v -> c.holdFullInventoryGuardAttack = v);
        bool(hold, entries, "holdDamageGuard", c.holdDamageGuard, defaults.holdDamageGuard, v -> c.holdDamageGuard = v);
        bool(hold, entries, "holdSlotGuard", c.holdSlotGuard, defaults.holdSlotGuard, v -> c.holdSlotGuard = v);
        bool(hold, entries, "holdMovementGuard", c.holdMovementGuard, defaults.holdMovementGuard, v -> c.holdMovementGuard = v);
        hold.addEntry(entries.startDoubleField(label("holdMovementTolerance"), c.holdMovementTolerance)
                .setMin(0.1).setMax(5.0).setDefaultValue(defaults.holdMovementTolerance)
                .setTooltip(tooltip("holdMovementTolerance")).setSaveConsumer(v -> c.holdMovementTolerance = v).build());
        bool(hold, entries, "holdTargetLostGuard", c.holdTargetLostGuard, defaults.holdTargetLostGuard, v -> c.holdTargetLostGuard = v);
        integer(hold, entries, "holdTargetLostTicks", c.holdTargetLostTicks, defaults.holdTargetLostTicks, 5, 400, v -> c.holdTargetLostTicks = v);

        ConfigCategory hud = category(builder, "hud_sound");
        bool(hud, entries, "hudEnabled", c.hudEnabled, defaults.hudEnabled, v -> c.hudEnabled = v);
        bool(hud, entries, "hudShowGuardPulse", c.hudShowGuardPulse, defaults.hudShowGuardPulse, v -> c.hudShowGuardPulse = v);
        bool(hud, entries, "hudShowPierce", c.hudShowPierce, defaults.hudShowPierce, v -> c.hudShowPierce = v);
        bool(hud, entries, "hudShowHold", c.hudShowHold, defaults.hudShowHold, v -> c.hudShowHold = v);
        integer(hud, entries, "hudOffsetX", c.hudOffsetX, defaults.hudOffsetX, -32, 32, v -> c.hudOffsetX = v);
        integer(hud, entries, "hudOffsetY", c.hudOffsetY, defaults.hudOffsetY, -32, 32, v -> c.hudOffsetY = v);
        integer(hud, entries, "hudOpacity", c.hudOpacity, defaults.hudOpacity, 10, 100, v -> c.hudOpacity = v);
        bool(hud, entries, "soundsEnabled", c.soundsEnabled, defaults.soundsEnabled, v -> c.soundsEnabled = v);
        integer(hud, entries, "soundVolume", c.soundVolume, defaults.soundVolume, 0, 100, v -> c.soundVolume = v);
        return builder.build();
    }

    private static ConfigCategory category(ConfigBuilder builder, String name) {
        return builder.getOrCreateCategory(Text.translatable("config.safeinteract.category." + name));
    }

    private static Text label(String name) { return Text.translatable("config.safeinteract.option." + name); }
    private static Text tooltip(String name) { return Text.translatable("config.safeinteract.option." + name + ".tooltip"); }

    private static void bool(ConfigCategory category, ConfigEntryBuilder entries, String name, boolean value,
                             boolean defaultValue, Consumer<Boolean> save) {
        category.addEntry(entries.startBooleanToggle(label(name), value).setDefaultValue(defaultValue)
                .setTooltip(tooltip(name)).setSaveConsumer(save).build());
    }

    private static void integer(ConfigCategory category, ConfigEntryBuilder entries, String name, int value,
                                int defaultValue, int min, int max, Consumer<Integer> save) {
        category.addEntry(entries.startIntSlider(label(name), value, min, max).setDefaultValue(defaultValue)
                .setTooltip(tooltip(name)).setSaveConsumer(save).build());
    }

    private static void strings(ConfigCategory category, ConfigEntryBuilder entries, String name, List<String> value,
                                List<String> defaultValue, Consumer<List<String>> save) {
        category.addEntry(entries.startStrList(label(name), value).setDefaultValue(defaultValue)
                .setTooltip(tooltip(name)).setSaveConsumer(save).build());
    }
}
