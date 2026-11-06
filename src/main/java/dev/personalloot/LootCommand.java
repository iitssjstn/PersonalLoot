package dev.personalloot;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.loot.LootTables;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class LootCommand implements TabExecutor {

    private final PersonalLootPlugin plugin;
    private final LootManager manager;

    public LootCommand(PersonalLootPlugin plugin, LootManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, String @NotNull [] args) {
        if (!sender.hasPermission("personalloot.admin")) {
            sender.sendMessage(plugin.msg("no-permission"));
            return true;
        }
        if (args.length == 0) {
            help(sender, label);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("reload")) {
            plugin.reload();
            sender.sendMessage(plugin.msg("reloaded"));
            return true;
        }
        if (!(sender instanceof Player p)) {
            sender.sendMessage(plugin.msg("player-only"));
            return true;
        }
        switch (sub) {
            case "info" -> info(p);
            case "set" -> set(p, args);
            case "reset" -> reset(p, args);
            case "remove" -> remove(p);
            default -> help(sender, label);
        }
        return true;
    }

    private void help(CommandSender s, String label) {
        s.sendMessage(PersonalLootPlugin.color("&6&lPersonalLoot &7- commando's"));
        s.sendMessage(PersonalLootPlugin.color("&e/" + label + " info &7- info over de kist waar je naar kijkt"));
        s.sendMessage(PersonalLootPlugin.color("&e/" + label + " set <loottable> &7- maak een (oude) kist weer persoonlijk"));
        s.sendMessage(PersonalLootPlugin.color("&e/" + label + " reset [speler] &7- loot opnieuw laten genereren"));
        s.sendMessage(PersonalLootPlugin.color("&e/" + label + " remove &7- maak er weer een gewone kist van"));
        s.sendMessage(PersonalLootPlugin.color("&e/" + label + " reload &7- config herladen"));
    }

    /** The container the player is looking at, registered or not. */
    private LootContainer target(Player p) {
        Entity entity = p.getTargetEntity(6);
        if (entity instanceof StorageMinecart cart) {
            return new LootContainer.CartContainer(cart);
        }
        Block block = p.getTargetBlockExact(6);
        if (block != null && LootContainer.isSupportedBlock(block.getType())) {
            LootContainer.BlockContainer resolved = manager.resolveBlock(block);
            return resolved != null ? resolved : new LootContainer.BlockContainer(block);
        }
        return null;
    }

    private void info(Player p) {
        LootContainer c = target(p);
        if (c == null) {
            p.sendMessage(plugin.msg("look-at-container"));
            return;
        }
        if (!manager.ensureRegistered(c)) {
            p.sendMessage(plugin.msg("not-personal"));
            return;
        }
        p.sendMessage(plugin.msg("info",
                "table", String.valueOf(manager.tableOf(c)),
                "count", String.valueOf(manager.lootedCount(c)),
                "you", manager.hasLooted(c, p.getUniqueId()) ? "ja" : "nee"));
    }

    private void set(Player p, String[] args) {
        if (args.length < 2) {
            p.sendMessage(plugin.msg("set-usage"));
            return;
        }
        LootContainer c = target(p);
        if (c == null) {
            p.sendMessage(plugin.msg("look-at-container"));
            return;
        }
        NamespacedKey key = parseTable(args[1]);
        if (key == null || Bukkit.getLootTable(key) == null) {
            p.sendMessage(plugin.msg("unknown-table", "table", args[1]));
            return;
        }
        if (c.hasRealContents()) {
            p.sendMessage(plugin.msg("not-empty"));
            return;
        }
        if (manager.isRegistered(c)) {
            manager.unregister(c);
        }
        manager.register(c, key, ThreadLocalRandom.current().nextLong());
        p.sendMessage(plugin.msg("set-done", "table", key.toString()));
    }

    private void reset(Player p, String[] args) {
        LootContainer c = target(p);
        if (c == null) {
            p.sendMessage(plugin.msg("look-at-container"));
            return;
        }
        if (!manager.ensureRegistered(c)) {
            p.sendMessage(plugin.msg("not-personal"));
            return;
        }
        UUID uuid = null;
        String who = "iedereen";
        if (args.length >= 2) {
            OfflinePlayer op = Bukkit.getOfflinePlayerIfCached(args[1]);
            if (op == null) {
                p.sendMessage(plugin.msg("unknown-player", "player", args[1]));
                return;
            }
            uuid = op.getUniqueId();
            who = op.getName() == null ? args[1] : op.getName();
        }
        manager.closeViewers(c.id());
        int n = manager.reset(c, uuid);
        p.sendMessage(plugin.msg("reset-done", "count", String.valueOf(n), "player", who));
    }

    private void remove(Player p) {
        LootContainer c = target(p);
        if (c == null) {
            p.sendMessage(plugin.msg("look-at-container"));
            return;
        }
        if (!manager.isRegistered(c)) {
            p.sendMessage(plugin.msg("not-personal"));
            return;
        }
        manager.unregister(c);
        p.sendMessage(plugin.msg("removed"));
    }

    /** Accepts "simple_dungeon", "chests/simple_dungeon" or "minecraft:chests/simple_dungeon". */
    static NamespacedKey parseTable(String input) {
        String s = input.toLowerCase(Locale.ROOT);
        if (!s.contains(":")) {
            if (!s.startsWith("chests/")) {
                s = "chests/" + s;
            }
            s = "minecraft:" + s;
        }
        return NamespacedKey.fromString(s);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, String @NotNull [] args) {
        List<String> out = new ArrayList<>();
        if (!sender.hasPermission("personalloot.admin")) {
            return out;
        }
        if (args.length == 1) {
            for (String s : List.of("info", "set", "reset", "remove", "reload")) {
                if (s.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    out.add(s);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            String typed = args[1].toLowerCase(Locale.ROOT);
            for (LootTables t : LootTables.values()) {
                String path = t.getKey().getKey();
                if (path.startsWith("chests/")) {
                    String shortName = path.substring("chests/".length());
                    if (shortName.startsWith(typed)) {
                        out.add(shortName);
                    }
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("reset")) {
            for (Player pl : Bukkit.getOnlinePlayers()) {
                if (pl.getName().toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    out.add(pl.getName());
                }
            }
        }
        return out;
    }
}
