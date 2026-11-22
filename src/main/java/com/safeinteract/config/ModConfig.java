package com.safeinteract.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.safeinteract.SafeInteractClient;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("safeinteract.json");
    private static ModConfig instance = new ModConfig();

    public boolean enabled = true;
    public boolean actionBarEnabled = true;
    public boolean buildGuardEnabled = true;
    public boolean entityGuardEnabled = true;
    public int holdThresholdTicks = 3;
    public List<String> guardedBlocks = new ArrayList<>();
    public List<String> ignoredBlocks = new ArrayList<>();
    public boolean durabilityGuardEnabled = true;
    public int durabilityThreshold = 5;
    public boolean durabilityGuardManualTools = true;
    public boolean durabilityGuardMining = true;
    public boolean entityPierceEnabled = true;
    public boolean pierceItemFrames = true;
    public boolean pierceArmorStands = true;
    public boolean pierceVillagers = true;
    public boolean pierceMinecarts = true;
    public boolean pierceBoats = true;
    public boolean piercePaintings = true;
    public boolean holdFullInventoryGuardUse = true;
    public boolean holdFullInventoryGuardAttack = false;
    public boolean holdDamageGuard = true;
    public boolean holdSlotGuard = true;
    public boolean holdMovementGuard = true;
    public double holdMovementTolerance = 0.5;
    public boolean holdTargetLostGuard = true;
    public int holdTargetLostTicks = 40;
    public boolean hudEnabled = true;
    public boolean hudShowGuardPulse = true;
    public boolean hudShowPierce = true;
    public boolean hudShowHold = true;
    public int hudOffsetX = 6;
    public int hudOffsetY = 6;
    public int hudOpacity = 100;
    public boolean soundsEnabled = true;
    public int soundVolume = 30;
    private transient Set<Identifier> guardedBlockIds = Set.of();
    private transient Set<Identifier> ignoredBlockIds = Set.of();

    public static ModConfig get() {
        return instance;
    }

    public static void load() {
        instance = new ModConfig();
        if (Files.exists(PATH)) {
            try (Reader reader = Files.newBufferedReader(PATH)) {
                ModConfig loaded = GSON.fromJson(reader, ModConfig.class);
                if (loaded == null) throw new IllegalArgumentException("Empty configuration");
                instance = loaded;
            } catch (Exception e) {
                SafeInteractClient.LOGGER.error("Could not read configuration; preserving broken file", e);
                try {
                    Files.move(PATH, PATH.resolveSibling("safeinteract.json.broken"), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException moveError) {
                    SafeInteractClient.LOGGER.error("Could not move broken configuration", moveError);
                    return;
                }
            }
        }
        save();
    }

    public static void save() {
        instance.sanitize();
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(PATH)) {
                GSON.toJson(instance, writer);
            }
        } catch (IOException e) {
            SafeInteractClient.LOGGER.error("Could not save configuration", e);
        }
    }

    public void sanitize() {
        holdThresholdTicks = clamp(holdThresholdTicks, 1, 20);
        durabilityThreshold = clamp(durabilityThreshold, 1, 50);
        holdMovementTolerance = Double.isFinite(holdMovementTolerance)
                ? Math.clamp(holdMovementTolerance, 0.1, 5.0) : 0.5;
        holdTargetLostTicks = clamp(holdTargetLostTicks, 5, 400);
        hudOffsetX = clamp(hudOffsetX, -32, 32);
        hudOffsetY = clamp(hudOffsetY, -32, 32);
        hudOpacity = clamp(hudOpacity, 10, 100);
        soundVolume = clamp(soundVolume, 0, 100);
        if (guardedBlocks == null) guardedBlocks = new ArrayList<>();
        if (ignoredBlocks == null) ignoredBlocks = new ArrayList<>();
        guardedBlockIds = parseIds(guardedBlocks);
        ignoredBlockIds = parseIds(ignoredBlocks);
    }

    private static int clamp(int value, int min, int max) {
        return Math.clamp(value, min, max);
    }

    private static Set<Identifier> parseIds(List<String> entries) {
        Set<Identifier> ids = new HashSet<>();
        for (String entry : entries) {
            Identifier id = entry == null ? null : Identifier.tryParse(entry.trim().toLowerCase(Locale.ROOT));
            if (id == null) SafeInteractClient.LOGGER.warn("Ignoring invalid block identifier: {}", entry);
            else ids.add(id);
        }
        return ids;
    }

    public boolean isGuarded(Identifier id) { return guardedBlockIds.contains(id); }
    public boolean isIgnored(Identifier id) { return ignoredBlockIds.contains(id); }
}
