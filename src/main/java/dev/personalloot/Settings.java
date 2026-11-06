package dev.personalloot;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Immutable snapshot of config.yml. */
public record Settings(
        Set<String> disabledWorlds,
        boolean includeChests,
        boolean includeBarrels,
        boolean includeMinecarts,
        long refreshMillis,
        String inventoryTitle,
        boolean protectBreak,
        boolean protectExplosions,
        boolean protectHoppers,
        boolean markersEnabled,
        boolean frameStyle,
        BlockData frameBlock,
        float frameThickness,
        BlockData shellBlock,
        boolean shellFullBright,
        boolean glow,
        Color glowColor,
        boolean iconForBlocks,
        boolean iconForMinecarts,
        Material iconItem,
        float iconScale,
        float markerViewRange,
        boolean particlesEnabled,
        Particle particle,
        int particleCount,
        double particleRange
) {

    public static Settings load(PersonalLootPlugin plugin) {
        FileConfiguration c = plugin.getConfig();
        var log = plugin.getLogger();

        Set<String> disabled = new HashSet<>();
        for (String w : c.getStringList("disabled-worlds")) {
            disabled.add(w.toLowerCase(Locale.ROOT));
        }

        BlockData shell;
        String shellName = c.getString("markers.shell-block", "minecraft:yellow_stained_glass");
        try {
            shell = Bukkit.createBlockData(shellName);
        } catch (IllegalArgumentException ex) {
            log.warning("Onbekend blok bij markers.shell-block: " + shellName + " (val terug op yellow_stained_glass)");
            shell = Material.YELLOW_STAINED_GLASS.createBlockData();
        }

        BlockData frame;
        String frameName = c.getString("markers.frame-block", "minecraft:gold_block");
        try {
            frame = Bukkit.createBlockData(frameName);
        } catch (IllegalArgumentException ex) {
            log.warning("Onbekend blok bij markers.frame-block: " + frameName + " (val terug op gold_block)");
            frame = Material.GOLD_BLOCK.createBlockData();
        }

        Color glowColor;
        String hex = c.getString("markers.glow-color", "FFD700").replace("#", "");
        try {
            glowColor = Color.fromRGB(Integer.parseInt(hex, 16) & 0xFFFFFF);
        } catch (NumberFormatException ex) {
            log.warning("Ongeldige markers.glow-color: " + hex);
            glowColor = Color.fromRGB(0xFFD700);
        }

        Material icon = Material.matchMaterial(c.getString("markers.icon.item", "minecraft:nether_star"));
        if (icon == null || !icon.isItem()) {
            log.warning("Onbekend item bij markers.icon.item, val terug op nether_star");
            icon = Material.NETHER_STAR;
        }

        Particle particle;
        String pName = c.getString("particles.type", "HAPPY_VILLAGER").toUpperCase(Locale.ROOT);
        try {
            particle = Particle.valueOf(pName);
        } catch (IllegalArgumentException ex) {
            log.warning("Onbekende particle: " + pName + " (val terug op HAPPY_VILLAGER)");
            particle = Particle.HAPPY_VILLAGER;
        }

        return new Settings(
                disabled,
                c.getBoolean("containers.chests", true),
                c.getBoolean("containers.barrels", true),
                c.getBoolean("containers.minecarts", true),
                Math.max(0L, (long) (c.getDouble("refresh-hours", 0) * 3_600_000L)),
                c.getString("inventory-title", "&6✦ &8Persoonlijke loot"),
                c.getBoolean("protection.prevent-break", true),
                c.getBoolean("protection.prevent-explosions", true),
                c.getBoolean("protection.prevent-hoppers", true),
                c.getBoolean("markers.enabled", true),
                !"shell".equalsIgnoreCase(c.getString("markers.style", "frame")),
                frame,
                (float) Math.max(0.02, Math.min(0.25, c.getDouble("markers.frame-thickness", 0.07))),
                shell,
                c.getBoolean("markers.full-bright", false),
                c.getBoolean("markers.glow", false),
                glowColor,
                c.getBoolean("markers.icon.blocks", false),
                c.getBoolean("markers.icon.minecarts", true),
                icon,
                (float) c.getDouble("markers.icon.scale", 0.5),
                (float) c.getDouble("markers.view-range", 1.0),
                c.getBoolean("particles.enabled", false),
                particle,
                Math.max(1, c.getInt("particles.count", 3)),
                c.getDouble("particles.range", 16)
        );
    }

    public boolean worldEnabled(org.bukkit.World world) {
        return !disabledWorlds.contains(world.getName().toLowerCase(Locale.ROOT));
    }
}
