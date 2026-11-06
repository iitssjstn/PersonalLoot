package dev.personalloot;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.Lidded;
import org.bukkit.block.TileState;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.loot.Lootable;
import org.bukkit.persistence.PersistentDataContainer;
import io.papermc.paper.persistence.PersistentDataContainerView;

import java.util.function.Consumer;

/**
 * A world-generated loot container: a chest/trapped chest/barrel block or a minecart with chest.
 * All per-player data lives in the container's own PersistentDataContainer, so it is saved
 * together with the world and needs no separate database.
 */
public abstract sealed class LootContainer permits LootContainer.BlockContainer, LootContainer.CartContainer {

    /** Stable id used for markers, viewer counts and open inventories. */
    public abstract String id();

    public abstract World world();

    /** Location used for the loot context and particles (centre of the container). */
    public abstract Location center();

    public abstract boolean isValid();

    /** Read-only view of the stored data. */
    public abstract PersistentDataContainerView data();

    /** Modify stored data; changes are written back so they are saved with the world. */
    public abstract void editData(Consumer<PersistentDataContainer> editor);

    /** The vanilla loot-table holder (only meaningful before the container is registered). */
    public abstract Lootable lootableSnapshot();

    /** Clear the vanilla loot table so the game never generates shared loot into it. */
    public abstract void clearVanillaLootTable();

    /** True when the real (shared) inventory has any items in it. */
    public abstract boolean hasRealContents();

    public abstract void openLid();

    public abstract void closeLid();

    public static boolean isSupportedBlock(Material type) {
        return type == Material.CHEST || type == Material.TRAPPED_CHEST || type == Material.BARREL;
    }

    public static String blockId(Block b) {
        return "b:" + b.getWorld().getName() + ":" + b.getX() + ":" + b.getY() + ":" + b.getZ();
    }

    // ------------------------------------------------------------------ blocks

    public static final class BlockContainer extends LootContainer {
        private final Block block;

        public BlockContainer(Block block) {
            this.block = block;
        }

        public Block block() {
            return block;
        }

        public boolean isBarrel() {
            return block.getType() == Material.BARREL;
        }

        @Override
        public String id() {
            return blockId(block);
        }

        @Override
        public World world() {
            return block.getWorld();
        }

        @Override
        public Location center() {
            return block.getLocation().add(0.5, 0.5, 0.5);
        }

        @Override
        public boolean isValid() {
            return isSupportedBlock(block.getType()) && block.getState(false) instanceof TileState;
        }

        @Override
        public PersistentDataContainerView data() {
            return ((TileState) block.getState(false)).getPersistentDataContainer();
        }

        @Override
        public void editData(Consumer<PersistentDataContainer> editor) {
            BlockState state = block.getState();
            if (!(state instanceof TileState tile)) {
                return;
            }
            editor.accept(tile.getPersistentDataContainer());
            tile.update(true, false);
        }

        @Override
        public Lootable lootableSnapshot() {
            return block.getState(false) instanceof Lootable l ? l : null;
        }

        @Override
        public void clearVanillaLootTable() {
            BlockState state = block.getState();
            if (state instanceof Lootable l && l.hasLootTable()) {
                l.clearLootTable();
                state.update(true, false);
            }
        }

        @Override
        public boolean hasRealContents() {
            BlockState state = block.getState(false);
            if (state instanceof Chest chest) {
                return !chest.getBlockInventory().isEmpty();
            }
            if (state instanceof Barrel barrel) {
                return !barrel.getInventory().isEmpty();
            }
            return false;
        }

        @Override
        public void openLid() {
            if (block.getState(false) instanceof Lidded lid && !lid.isOpen()) {
                lid.open();
            }
        }

        @Override
        public void closeLid() {
            if (block.getState(false) instanceof Lidded lid && lid.isOpen()) {
                lid.close();
            }
        }
    }

    // ------------------------------------------------------------------ minecarts

    public static final class CartContainer extends LootContainer {
        private final StorageMinecart cart;

        public CartContainer(StorageMinecart cart) {
            this.cart = cart;
        }

        public StorageMinecart cart() {
            return cart;
        }

        @Override
        public String id() {
            return "e:" + cart.getUniqueId();
        }

        @Override
        public World world() {
            return cart.getWorld();
        }

        @Override
        public Location center() {
            return cart.getLocation().add(0, 0.5, 0);
        }

        @Override
        public boolean isValid() {
            return cart.isValid() && !cart.isDead();
        }

        @Override
        public PersistentDataContainerView data() {
            return cart.getPersistentDataContainer();
        }

        @Override
        public void editData(Consumer<PersistentDataContainer> editor) {
            editor.accept(cart.getPersistentDataContainer());
        }

        @Override
        public Lootable lootableSnapshot() {
            return cart;
        }

        @Override
        public void clearVanillaLootTable() {
            if (cart.hasLootTable()) {
                cart.clearLootTable();
            }
        }

        @Override
        public boolean hasRealContents() {
            return !cart.getInventory().isEmpty();
        }

        @Override
        public void openLid() {
            // Minecarts have no lid.
        }

        @Override
        public void closeLid() {
        }
    }
}
