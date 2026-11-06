package dev.personalloot;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class PersonalLootPlugin extends JavaPlugin {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private NamespacedKey keyTable;
    private NamespacedKey keySeed;
    private Settings settings;
    private LootManager manager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        keyTable = new NamespacedKey(this, "table");
        keySeed = new NamespacedKey(this, "seed");
        settings = Settings.load(this);

        manager = new LootManager(this);
        Bukkit.getPluginManager().registerEvents(new LootListener(this, manager), this);

        PluginCommand cmd = getCommand("personalloot");
        if (cmd != null) {
            LootCommand executor = new LootCommand(this, manager);
            cmd.setExecutor(executor);
            cmd.setTabCompleter(executor);
        }

        // Process chunks that were already loaded before the plugin enabled.
        Bukkit.getScheduler().runTask(this, manager::scanLoadedChunks);
        manager.startTasks();
    }

    @Override
    public void onDisable() {
        if (manager != null) {
            manager.shutdown();
        }
    }

    public void reload() {
        reloadConfig();
        settings = Settings.load(this);
        manager.rebuildMarkers();
    }

    public LootManager manager() {
        return manager;
    }

    public Settings settings() {
        return settings;
    }

    public NamespacedKey keyTable() {
        return keyTable;
    }

    public NamespacedKey keySeed() {
        return keySeed;
    }

    public NamespacedKey playerKey(java.util.UUID uuid) {
        return new NamespacedKey(this, "p_" + uuid);
    }

    public NamespacedKey timeKey(java.util.UUID uuid) {
        return new NamespacedKey(this, "t_" + uuid);
    }

    public static Component color(String legacy) {
        return LEGACY.deserialize(legacy == null ? "" : legacy);
    }

    public Component msg(String path, String... replacements) {
        String raw = getConfig().getString("messages." + path, path);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            raw = raw.replace("{" + replacements[i] + "}", replacements[i + 1]);
        }
        String prefix = getConfig().getString("messages.prefix", "");
        return color(prefix + raw);
    }
}
