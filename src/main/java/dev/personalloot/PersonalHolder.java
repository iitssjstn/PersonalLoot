package dev.personalloot;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/** Marks an inventory as one player's private copy of a loot container. */
public final class PersonalHolder implements InventoryHolder {

    private final LootContainer container;
    private final UUID owner;
    private Inventory inventory;

    public PersonalHolder(LootContainer container, UUID owner) {
        this.container = container;
        this.owner = owner;
    }

    public LootContainer container() {
        return container;
    }

    public UUID owner() {
        return owner;
    }

    void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
