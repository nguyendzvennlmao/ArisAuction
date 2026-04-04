package me.aris.arisauction.gui;

import me.aris.arisauction.ArisAuction;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ConfirmSellGUI {
    private final ArisAuction plugin;
    private final Player player;
    private final ItemStack item;
    private final double price;
    private Inventory inventory;
    private boolean confirmed;

    public ConfirmSellGUI(ArisAuction plugin, Player player, ItemStack item, double price) {
        this.plugin = plugin;
        this.player = player;
        this.item = item != null ? item.clone() : null;
        this.price = price;
        this.confirmed = false;
    }

    public void open() {
        createInventory();
        fillItems();
        player.openInventory(inventory);
    }

    private void createInventory() {
        String title = plugin.getConfigManager().getGUI("confirmsell").getString("GUI-Title", "&7CONFIRM MENU");
        title = plugin.getConfigManager().colorize(title);
        int size = plugin.getConfigManager().getGUI("confirmsell").getInt("size", 27);
        inventory = Bukkit.createInventory(null, size, title);
    }

    private void fillItems() {
        List<Integer> slots = plugin.getConfigManager().getGUI("confirmsell").getIntegerList("Listing-Slots");
        if (slots.isEmpty()) slots.add(13);
        for (int slot : slots) {
            inventory.setItem(slot, createDisplayItem());
        }
        setFiller();
        setControlItems();
    }

    private ItemStack createDisplayItem() {
        if (item == null) return new ItemStack(Material.AIR);
        ItemStack display = item.clone();
        ItemMeta meta = display.getItemMeta();
        List<String> lore = plugin.getConfigManager().getGUI("confirmsell").getStringList("Product-Lore");
        List<String> colored = new ArrayList<>();
        double tax = plugin.getConfigManager().getConfig().getDouble("tax-rate", 0.0);
        for (String line : lore) {
            line = line.replace("%price%", plugin.getEconomyManager().format(price));
            line = line.replace("%tax%", plugin.getEconomyManager().format(price * tax));
            colored.add(plugin.getConfigManager().colorize(line));
        }
        if (meta.hasLore() && meta.getLore() != null) colored.addAll(0, meta.getLore());
        meta.setLore(colored);
        display.setItemMeta(meta);
        return display;
    }

    private void setFiller() {
        String name = plugin.getConfigManager().getGUI("confirmsell").getString("Filler.name", "");
        String mat = plugin.getConfigManager().getGUI("confirmsell").getString("Filler.material", "BLACK_STAINED_GLASS_PANE");
        Material material = Material.getMaterial(mat);
        if (material == null) material = Material.BLACK_STAINED_GLASS_PANE;
        ItemStack filler = new ItemStack(material);
        ItemMeta meta = filler.getItemMeta();
        if (meta != null) meta.setDisplayName(plugin.getConfigManager().colorize(name));
        filler.setItemMeta(meta);
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) inventory.setItem(i, filler);
        }
    }

    private void setControlItems() {
        int acceptSlot = plugin.getConfigManager().getGUI("confirmsell").getInt("Accept.slot", 11);
        String acceptName = plugin.getConfigManager().getGUI("confirmsell").getString("Accept.name", "&aCONFIRM");
        String acceptMat = plugin.getConfigManager().getGUI("confirmsell").getString("Accept.material", "GREEN_STAINED_GLASS_PANE");
        setItem(acceptSlot, acceptMat, acceptName, plugin.getConfigManager().getGUI("confirmsell").getStringList("Accept.lore"));

        int refuseSlot = plugin.getConfigManager().getGUI("confirmsell").getInt("Refuse.slot", 15);
        String refuseName = plugin.getConfigManager().getGUI("confirmsell").getString("Refuse.name", "&cCANCEL");
        String refuseMat = plugin.getConfigManager().getGUI("confirmsell").getString("Refuse.material", "RED_STAINED_GLASS_PANE");
        setItem(refuseSlot, refuseMat, refuseName, plugin.getConfigManager().getGUI("confirmsell").getStringList("Refuse.lore"));
    }

    private void setItem(int slot, String matName, String displayName, List<String> lore) {
        Material mat = Material.getMaterial(matName);
        if (mat == null) mat = Material.STONE;
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(plugin.getConfigManager().colorize(displayName));
            if (lore != null) meta.setLore(plugin.getConfigManager().colorizeList(lore));
            item.setItemMeta(meta);
        }
        inventory.setItem(slot, item);
    }

    private void playSound(String soundKey) {
        String soundName = plugin.getConfigManager().getConfig().getString("sounds." + soundKey);
        if (soundName != null && !soundName.isEmpty()) {
            try {
                Sound sound = Sound.valueOf(soundName);
                player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
            } catch (IllegalArgumentException e) {
            }
        }
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        int accept = plugin.getConfigManager().getGUI("confirmsell").getInt("Accept.slot", 11);
        int refuse = plugin.getConfigManager().getGUI("confirmsell").getInt("Refuse.slot", 15);
        if (slot == accept && !confirmed && item != null) {
            confirmed = true;
            playSound("confirm");
            plugin.getAuctionManager().sellItem(player, item, price);
            player.closeInventory();
        } else if (slot == refuse) {
            playSound("cancel");
            player.closeInventory();
            player.sendMessage(plugin.getConfigManager().getMessage("item-cancelled"));
        }
    }
            }
