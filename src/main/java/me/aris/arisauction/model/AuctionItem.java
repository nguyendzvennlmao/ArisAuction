package me.aris.arisauction.model;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.Serializable;
import java.util.UUID;

public class AuctionItem implements Serializable {
    private static final long serialVersionUID = 1L;
    private final UUID id;
    private final UUID seller;
    private final ItemStack itemStack;
    private final double price;
    private final long listedTime;
    private final long expiryTime;

    public AuctionItem(UUID id, UUID seller, ItemStack itemStack, double price, long listedTime, long expiryTime) {
        this.id = id;
        this.seller = seller;
        this.itemStack = itemStack.clone();
        this.price = price;
        this.listedTime = listedTime;
        this.expiryTime = expiryTime;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSeller() {
        return seller;
    }

    public ItemStack getItemStack() {
        return itemStack.clone();
    }

    public double getPrice() {
        return price;
    }

    public long getListedTime() {
        return listedTime;
    }

    public long getExpiryTime() {
        return expiryTime;
    }
    
    public String getItemName() {
        if (itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName()) {
            return itemStack.getItemMeta().getDisplayName();
        }
        return itemStack.getType().toString();
    }
}
