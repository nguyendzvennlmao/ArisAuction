package me.aris.arisauction.model;

import org.bukkit.inventory.ItemStack;

import java.io.Serializable;
import java.util.UUID;

public class Transaction implements Serializable {
    private static final long serialVersionUID = 1L;
    private final ItemStack item;
    private final UUID seller;
    private final UUID buyer;
    private final double price;
    private final long time;

    public Transaction(ItemStack item, UUID seller, UUID buyer, double price, long time) {
        this.item = item.clone();
        this.seller = seller;
        this.buyer = buyer;
        this.price = price;
        this.time = time;
    }

    public ItemStack getItem() {
        return item.clone();
    }

    public UUID getSeller() {
        return seller;
    }

    public UUID getBuyer() {
        return buyer;
    }

    public double getPrice() {
        return price;
    }

    public long getTime() {
        return time;
    }
}
