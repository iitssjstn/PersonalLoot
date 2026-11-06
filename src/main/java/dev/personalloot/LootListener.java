package dev.personalloot;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.ArrayList;
import java.util.List;

public final class LootListener implements Listener {

    private final PersonalLootPlugin plugin;
    private final LootManager manager;

    public LootListener(PersonalLootPlugin plugin, LootManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    // ------------------------------------------------------------------ chunks / entities

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent e) {
        var chunk = e.getChunk();
        Bukkit.getScheduler().runTask(plugin, () -> manager.scanChunkBlocks(chunk));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkUnload(ChunkUnloadEvent e) {
        manager.unloadChunk(e.getChunk());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntitiesLoad(EntitiesLoadEvent e) {
        List<Entity> carts = new ArrayList<>();
        for (Entity entity : e.getEntities()) {
            if (entity instanceof StorageMinecart) {
                carts.add(entity);
            }
        }
        if (!carts.isEmpty()) {
            Bukkit.getScheduler().runTask(plugin, () -> manager.scanEntities(carts));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntitiesUnload(EntitiesUnloadEvent e) {
        manager.unloadEntities(e.getEntities());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (p.isOnline()) {
                manager.updateVisibility(p);
            }
        }, 5L);
    }

    // ------------------------------------------------------------------ opening

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getClickedBlock() == null) {
            return;
        }
        Block block = e.getClickedBlock();
        if (!LootContainer.isSupportedBlock(block.getType())) {
            return;
        }
        if (e.useInteractedBlock() == Event.Result.DENY) {
            return; // another plugin (claims, regions...) blocked it
        }
        Player p = e.getPlayer();
        if (p.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        // Sneaking with an item in hand places the item instead of opening.
        if (p.isSneaking() && !(p.getInventory().getItemInMainHand().isEmpty()
                && p.getInventory().getItemInOffHand().isEmpty())) {
            return;
        }
        LootContainer.BlockContainer c = manager.resolveBlock(block);
        if (c == null) {
            return;
        }
        e.setUseInteractedBlock(Event.Result.DENY);
        e.setUseItemInHand(Event.Result.DENY);
        if (e.getHand() == EquipmentSlot.HAND) {
            manager.open(p, c);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent e) {
        if (!(e.getRightClicked() instanceof StorageMinecart)) {
            return;
        }
        if (e.getPlayer().getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        LootContainer.CartContainer c = manager.resolveCart(e.getRightClicked());
        if (c == null) {
            return;
        }
        e.setCancelled(true);
        if (e.getHand() == EquipmentSlot.HAND) {
            manager.open(e.getPlayer(), c);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent e) {
        if (e.getInventory().getHolder(false) instanceof PersonalHolder holder
                && e.getPlayer() instanceof Player p) {
            manager.save(holder, p);
        }
    }

    // ------------------------------------------------------------------ protection

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHopper(InventoryMoveItemEvent e) {
        if (!plugin.settings().protectHoppers()) {
            return;
        }
        if (manager.isProtectedInventory(e.getSource()) || manager.isProtectedInventory(e.getDestination())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Block block = e.getBlock();
        if (!LootContainer.isSupportedBlock(block.getType())) {
            return;
        }
        LootContainer.BlockContainer c = new LootContainer.BlockContainer(block);
        if (!manager.isRegistered(c)) {
            return;
        }
        Player p = e.getPlayer();
        boolean admin = p.hasPermission("personalloot.break");
        if (plugin.settings().protectBreak() && !(admin && p.isSneaking())) {
            e.setCancelled(true);
            p.sendMessage(plugin.msg(admin ? "cannot-break-admin" : "cannot-break"));
            return;
        }
        manager.closeViewers(c.id());
        manager.removeMarker(c.id());
        if (admin) {
            p.sendMessage(plugin.msg("broken"));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent e) {
        if (plugin.settings().protectExplosions()) {
            e.blockList().removeIf(this::isProtectedBlock);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent e) {
        if (plugin.settings().protectExplosions()) {
            e.blockList().removeIf(this::isProtectedBlock);
        }
    }

    private boolean isProtectedBlock(Block b) {
        return LootContainer.isSupportedBlock(b.getType())
                && manager.isRegistered(new LootContainer.BlockContainer(b));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCartDamage(VehicleDamageEvent e) {
        if (e.getVehicle() instanceof StorageMinecart cart && blockCartDestruction(cart, e.getAttacker())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCartDestroy(VehicleDestroyEvent e) {
        if (!(e.getVehicle() instanceof StorageMinecart cart)) {
            return;
        }
        if (blockCartDestruction(cart, e.getAttacker())) {
            e.setCancelled(true);
            return;
        }
        LootContainer.CartContainer c = new LootContainer.CartContainer(cart);
        if (manager.isRegistered(c)) {
            manager.closeViewers(c.id());
            manager.removeMarker(c.id());
        }
    }

    private boolean blockCartDestruction(StorageMinecart cart, Entity attacker) {
        if (!plugin.settings().protectBreak()) {
            return false;
        }
        if (!manager.isRegistered(new LootContainer.CartContainer(cart))) {
            return false;
        }
        if (attacker instanceof Player p && p.hasPermission("personalloot.break") && p.isSneaking()) {
            return false;
        }
        if (attacker instanceof Player p) {
            p.sendActionBar(plugin.msg(p.hasPermission("personalloot.break") ? "cannot-break-admin" : "cannot-break"));
        }
        return true;
    }
}
