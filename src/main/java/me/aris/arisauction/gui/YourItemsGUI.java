package me.aris.arisauction.gui;

import me.aris.arisauction.ArisAuction;
import me.aris.arisauction.model.AuctionItem;
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
import java.util.concurrent.TimeUnit;

public class YourItemsGUI {
    private final ArisAuction plugin;
    private final Player player;
    private int page;
    private List<AuctionItem> items;
    private Inventory inventory;

    public YourItemsGUI(ArisAuction plugin, Player player, int page) {
        this.plugin = plugin;
        this.player = player;
        this.page = page;
        this.items = new ArrayList<>();
    }

    public void open() {
        loadItems();
        createInventory();
        fillItems();
        player.openInventory(inventory);
    }

    private void loadItems() {
        items = plugin.getAuctionManager().getAuctionsByPlayer(player.getUniqueId());
    }

    private void createInventory() {
        int totalPages = (int) Math.ceil(items.size() / 45.0);
        String title = plugin.getConfigManager().getGUI("youritems").getString("GUI-Title", "&b✹ &7My Items (%page%/%max%)");
        title = title.replace("%page%", String.valueOf(page)).replace("%max%", String.valueOf(Math.max(1, totalPages)));
        title = plugin.getConfigManager().colorize(title);
        int size = plugin.getConfigManager().getGUI("youritems").getInt("size", 54);
        inventory = Bukkit.createInventory(null, size, title);
    }

    private void fillItems() {
        List<Integer> listingSlots = plugin.getConfigManager().getGUI("youritems").getIntegerList("Listing-Slots");
        int startIndex = (page - 1) * 45;
        for (int i = 0; i < listingSlots.size() && startIndex + i < items.size(); i++) {
            AuctionItem auctionItem = items.get(startIndex + i);
            ItemStack displayItem = createDisplayItem(auctionItem);
            inventory.setItem(listingSlots.get(i), displayItem);
        }
        setFiller();
        setControlItems();
    }

    private ItemStack createDisplayItem(AuctionItem auctionItem) {
        ItemStack item = auctionItem.getItemStack().clone();
        ItemMeta meta = item.getItemMeta();
        List<String> lore = plugin.getConfigManager().getGUI("youritems").getStringList("Product-Lore");
        List<String> coloredLore = new ArrayList<>();
        for (String line : lore) {
            line = line.replace("%seller%", Bukkit.getOfflinePlayer(auctionItem.getSeller()).getName());
            line = line.replace("%time%", formatTime(auctionItem.getExpiryTime()));
            line = line.replace("%price%", plugin.getEconomyManager().format(auctionItem.getPrice()));
            coloredLore.add(plugin.getConfigManager().colorize(line));
        }
        if (meta.hasLore() && meta.getLore() != null) {
            coloredLore.addAll(0, meta.getLore());
        }
        meta.setLore(coloredLore);
        item.setItemMeta(meta);
        return item;
    }

    private String formatTime(long expiryTime) {
        long remaining = expiryTime - System.currentTimeMillis();
        if (remaining <= 0) return "Expired";
        long days = TimeUnit.MILLISECONDS.toDays(remaining);
        long hours = TimeUnit.MILLISECONDS.toHours(remaining) % 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(remaining) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(remaining) % 60;
        String daysStr = plugin.getConfigManager().getConfig().getString("time-format.days", "d");
        String hoursStr = plugin.getConfigManager().getConfig().getString("time-format.hours", "h");
        String minutesStr = plugin.getConfigManager().getConfig().getString("time-format.minutes", "m");
        String secondsStr = plugin.getConfigManager().getConfig().getString("time-format.seconds", "s");
        if (days > 0) return days + daysStr + " " + hours + hoursStr;
        if (hours > 0) return hours + hoursStr + " " + minutes + minutesStr;
        if (minutes > 0) return minutes + minutesStr + " " + seconds + secondsStr;
        return seconds + secondsStr;
    }

    private void setFiller() {
        String fillerName = plugin.getConfigManager().getGUI("youritems").getString("Filler.name", "");
        String fillerMaterial = plugin.getConfigManager().getGUI("youritems").getString("Filler.material", "BLACK_STAINED_GLASS_PANE");
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
        String backName = plugin.getConfigManager().getGUI("youritems").getString("Previous-Page-Item.name", "&e&lPREVIOUS PAGE");
        String backMaterial = plugin.getConfigManager().getGUI("youritems").getString("Previous-Page-Item.material", "ARROW");
        int backSlot = plugin.getConfigManager().getGUI("youritems").getInt("Previous-Page-Item.slot", 48);
        List<String> backLore = plugin.getConfigManager().getGUI("youritems").getStringList("Previous-Page-Item.lore");
        setItem(backSlot, backMaterial, backName, backLore);

        String backToAuctionName = plugin.getConfigManager().getGUI("youritems").getString("Back-Item.name", "&e&lGO BACK");
        String backToAuctionMaterial = plugin.getConfigManager().getGUI("youritems").getString("Back-Item.material", "RED_STAINED_GLASS_PANE");
        int backToAuctionSlot = plugin.getConfigManager().getGUI("youritems").getInt("Back-Item.slot", 49);
        List<String> backToAuctionLore = plugin.getConfigManager().getGUI("youritems").getStringList("Back-Item.lore");
        setItem(backToAuctionSlot, backToAuctionMaterial, backToAuctionName, backToAuctionLore);

        String nextName = plugin.getConfigManager().getGUI("youritems").getString("Next-Page-Item.name", "&e&lNEXT PAGE");
        String nextMaterial = plugin.getConfigManager().getGUI("youritems").getString("Next-Page-Item.material", "ARROW");
        int nextSlot = plugin.getConfigManager().getGUI("youritems").getInt("Next-Page-Item.slot", 50);
        List<String> nextLore = plugin.getConfigManager().getGUI("youritems").getStringList("Next-Page-Item.lore");
        setItem(nextSlot, nextMaterial, nextName, nextLore);

        String myItemsName = plugin.getConfigManager().getGUI("youritems").getString("MyItems-Item.name", "&e&lMY ITEMS");
        String myItemsMaterial = plugin.getConfigManager().getGUI("youritems").getString("MyItems-Item.material", "CHEST");
        int myItemsSlot = plugin.getConfigManager().getGUI("youritems").getInt("MyItems-Item.slot", 53);
        List<String> myItemsLore = plugin.getConfigManager().getGUI("youritems").getStringList("MyItems-Item.lore");
        setItem(myItemsSlot, myItemsMaterial, myItemsName, myItemsLore);
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

        int backSlot = plugin.getConfigManager().getGUI("youritems").getInt("Previous-Page-Item.slot", 48);
        int nextSlot = plugin.getConfigManager().getGUI("youritems").getInt("Next-Page-Item.slot", 50);
        int backToAuctionSlot = plugin.getConfigManager().getGUI("youritems").getInt("Back-Item.slot", 49);
        int myItemsSlot = plugin.getConfigManager().getGUI("youritems").getInt("MyItems-Item.slot", 53);

        if (slot == backSlot && page > 1) {
            playSound("page");
            new YourItemsGUI(plugin, player, page - 1).open();
            return;
        }
        if (slot == nextSlot) {
            int totalPages = (int) Math.ceil(items.size() / 45.0);
            if (page < totalPages) {
                playSound("page");
                new YourItemsGUI(plugin, player, page + 1).open();
            }
            return;
        }
        if (slot == backToAuctionSlot) {
            playSound("click");
            new AuctionGUI(plugin, player, 1, "Recently Listed").open();
            return;
        }
        if (slot == myItemsSlot) {
            playSound("click");
            new YourItemsGUI(plugin, player, 1).open();
            return;
        }

        List<Integer> listingSlots = plugin.getConfigManager().getGUI("youritems").getIntegerList("Listing-Slots");
        int itemIndex = listingSlots.indexOf(slot);
        int startIndex = (page - 1) * 45;
        if (itemIndex >= 0 && startIndex + itemIndex < items.size()) {
            AuctionItem auctionItem = items.get(startIndex + itemIndex);
            if (event.isRightClick()) {
                playSound("cancel");
                plugin.getAuctionManager().cancelItem(player, auctionItem.getId());
                new YourItemsGUI(plugin, player, page).open();
            }
        }
    }
            }
