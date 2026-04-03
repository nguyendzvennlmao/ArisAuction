package me.aris.arisauction.gui;

import me.aris.arisauction.ArisAuction;
import org.bukkit.Bukkit;
import org.bukkit.Material;
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

    public ConfirmSellGUI(ArisAuction plugin, Player player, ItemStack item, double price) {
        this.plugin = plugin;
        this.player = player;
        this.item = item;
        this.price = price;
    }

    public void open() {
        createInventory();
        fillItems();
        player.openInventory(inventory);
    }

    private void createInventory() {
        String title = plugin.getConfigManager().getGUI("confirmsell").getString("GUI-Title", "&7ᴄᴏɴꜰɪʀᴍ ᴍᴇɴᴜ");
        title = plugin.getConfigManager().colorize(title);
        int size = plugin.getConfigManager().getGUI("confirmsell").getInt("size", 27);
        inventory = Bukkit.createInventory(null, size, title);
    }

    private void fillItems() {
        List<Integer> listingSlots = plugin.getConfigManager().getGUI("confirmsell").getIntegerList("Listing-Slots");
        
        for (int slot : listingSlots) {
            ItemStack displayItem = createDisplayItem();
            inventory.setItem(slot, displayItem);
        }
        
        setFiller();
        setControlItems();
    }

    private ItemStack createDisplayItem() {
        ItemStack displayItem = item.clone();
        ItemMeta meta = displayItem.getItemMeta();
        
        List<String> lore = plugin.getConfigManager().getGUI("confirmsell").getStringList("Product-Lore");
        List<String> coloredLore = new ArrayList<>();
        double tax = plugin.getConfigManager().getConfig().getDouble("tax-rate", 0.0);
        double taxAmount = price * tax;
        double receiveAmount = price - taxAmount;
        
        for (String line : lore) {
            line = line.replace("%price%", plugin.getEconomyManager().format(price));
            line = line.replace("%tax%", plugin.getEconomyManager().format(taxAmount));
            line = line.replace("%receive%", plugin.getEconomyManager().format(receiveAmount));
            coloredLore.add(plugin.getConfigManager().colorize(line));
        }
        
        if (meta.hasLore()) {
            List<String> originalLore = meta.getLore();
            if (originalLore != null) {
                coloredLore.addAll(0, originalLore);
            }
        }
        
        meta.setLore(coloredLore);
        displayItem.setItemMeta(meta);
        return displayItem;
    }

    private void setFiller() {
        String fillerName = plugin.getConfigManager().getGUI("confirmsell").getString("Filler.name", "");
        String fillerMaterial = plugin.getConfigManager().getGUI("confirmsell").getString("Filler.material", "BLACK_STAINED_GLASS_PANE");
        Material material = Material.getMaterial(fillerMaterial);
        if (material == null) material = Material.BLACK_STAINED_GLASS_PANE;
        
        ItemStack filler = new ItemStack(material);
        ItemMeta meta = filler.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(plugin.getConfigManager().colorize(fillerName));
            filler.setItemMeta(meta);
        }
        
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, filler);
            }
        }
    }

    private void setControlItems() {
        String acceptName = plugin.getConfigManager().getGUI("confirmsell").getString("Accept.name", "&aᴄᴏɴꜰɪʀᴍ");
        String acceptMaterial = plugin.getConfigManager().getGUI("confirmsell").getString("Accept.material", "GREEN_STAINED_GLASS_PANE");
        int acceptSlot = plugin.getConfigManager().getGUI("confirmsell").getInt("Accept.slot", 11);
        List<String> acceptLore = plugin.getConfigManager().getGUI("confirmsell").getStringList("Accept.lore");
        setItem(acceptSlot, acceptMaterial, acceptName, acceptLore);
        
        String refuseName = plugin.getConfigManager().getGUI("confirmsell").getString("Refuse.name", "&cᴄᴀɴᴄᴇʟ");
        String refuseMaterial = plugin.getConfigManager().getGUI("confirmsell").getString("Refuse.material", "RED_STAINED_GLASS_PANE");
        int refuseSlot = plugin.getConfigManager().getGUI("confirmsell").getInt("Refuse.slot", 15);
        List<String> refuseLore = plugin.getConfigManager().getGUI("confirmsell").getStringList("Refuse.lore");
        setItem(refuseSlot, refuseMaterial, refuseName, refuseLore);
    }

    private void setItem(int slot, String materialName, String displayName, List<String> lore) {
        Material material = Material.getMaterial(materialName);
        if (material == null) material = Material.STONE;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(plugin.getConfigManager().colorize(displayName));
            if (lore != null && !lore.isEmpty()) {
                List<String> coloredLore = new ArrayList<>();
                for (String line : lore) {
                    coloredLore.add(plugin.getConfigManager().colorize(line));
                }
                meta.setLore(coloredLore);
            }
            item.setItemMeta(meta);
        }
        inventory.setItem(slot, item);
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        
        int acceptSlot = plugin.getConfigManager().getGUI("confirmsell").getInt("Accept.slot", 11);
        int refuseSlot = plugin.getConfigManager().getGUI("confirmsell").getInt("Refuse.slot", 15);
        
        if (slot == acceptSlot) {
            plugin.getAuctionManager().sellItem(player, item, price);
            player.closeInventory();
        } else if (slot == refuseSlot) {
            player.closeInventory();
            player.sendMessage(plugin.getConfigManager().getMessage("item-cancelled"));
        }
    }
  }
