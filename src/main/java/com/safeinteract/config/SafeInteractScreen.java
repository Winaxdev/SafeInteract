package com.safeinteract.config;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import java.util.function.Consumer;

public class SafeInteractScreen extends OptionsSubScreen {
    public SafeInteractScreen(Screen parent) {
        super(parent, Minecraft.getInstance().options, Component.translatable("config.safeinteract.title"));
    }

    @Override
    protected void addOptions() {
        ModConfig c = ModConfig.get();
        list.addSmall(
                bool("enabled", c.enabled, v -> c.enabled = v),
                bool("actionBarEnabled", c.actionBarEnabled, v -> c.actionBarEnabled = v),
                bool("buildGuardEnabled", c.buildGuardEnabled, v -> c.buildGuardEnabled = v),
                bool("entityGuardEnabled", c.entityGuardEnabled, v -> c.entityGuardEnabled = v),
                bool("entityPierceEnabled", c.entityPierceEnabled, v -> c.entityPierceEnabled = v),
                bool("durabilityGuardEnabled", c.durabilityGuardEnabled, v -> c.durabilityGuardEnabled = v),
                integer("durabilityThreshold", c.durabilityThreshold, 1, 50, "", v -> c.durabilityThreshold = v),
                bool("durabilityGuardManualTools", c.durabilityGuardManualTools, v -> c.durabilityGuardManualTools = v),
                bool("durabilityGuardMining", c.durabilityGuardMining, v -> c.durabilityGuardMining = v),
                bool("holdFullInventoryGuardUse", c.holdFullInventoryGuardUse, v -> c.holdFullInventoryGuardUse = v),
                bool("holdDamageGuard", c.holdDamageGuard, v -> c.holdDamageGuard = v),
                bool("holdSlotGuard", c.holdSlotGuard, v -> c.holdSlotGuard = v),
                bool("holdMovementGuard", c.holdMovementGuard, v -> c.holdMovementGuard = v),
                bool("holdTargetLostGuard", c.holdTargetLostGuard, v -> c.holdTargetLostGuard = v),
                bool("hudEnabled", c.hudEnabled, v -> c.hudEnabled = v),
                integer("hudOpacity", c.hudOpacity, 10, 100, "%", v -> c.hudOpacity = v),
                bool("soundsEnabled", c.soundsEnabled, v -> c.soundsEnabled = v),
                integer("soundVolume", c.soundVolume, 0, 100, "%", v -> c.soundVolume = v)
        );
    }

    private static OptionInstance<Boolean> bool(String name, boolean value, Consumer<Boolean> save) {
        String key = "config.safeinteract.option." + name;
        return OptionInstance.createBoolean(key, OptionInstance.cachedConstantTooltip(Component.translatable(key + ".tooltip")), value, save::accept);
    }

    private static OptionInstance<Integer> integer(String name, int value, int min, int max, String suffix, Consumer<Integer> save) {
        String key = "config.safeinteract.option." + name;
        return new OptionInstance<>(key, OptionInstance.cachedConstantTooltip(Component.translatable(key + ".tooltip")),
                (text, v) -> Options.genericValueLabel(text, Component.literal(v + suffix)),
                new OptionInstance.IntRange(min, max), value, save::accept);
    }

    @Override
    public void removed() {
        super.removed();
        ModConfig.save();
    }

    @Override
    protected void addFooter() {
        if (!FabricLoader.getInstance().isModLoaded("cloth-config")) {
            super.addFooter();
            return;
        }
        LinearLayout row = layout.addToFooter(LinearLayout.horizontal().spacing(8));
        row.addChild(Button.builder(Component.translatable("config.safeinteract.advanced"),
                b -> minecraft.gui.setScreen(ClothConfigScreen.create(this))).width(150).build());
        row.addChild(Button.builder(CommonComponents.GUI_DONE, b -> onClose()).width(150).build());
    }
}
