package me.aris.arisauction.gui;

import me.aris.arisauction.ArisAuction;
import me.aris.arisauction.model.Transaction;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TransactionsGUI {
    private final ArisAuction plugin;
    private final Player player;
    private int page;
    private List<Transaction> transactions;
    private Inventory inventory;

    public TransactionsGUI(ArisAuction plugin, Player player, int page) {
        this.plugin = plugin;
        this.player = player;
        this.page = page;
        this.transactions = new ArrayList<>();
    }

    public void open() {
        loadTransactions();
        createInventory();
        fillItems();
        player.openInventory(inventory);
    }

    private void loadTransactions() {
        transactions = plugin.getAuctionManager().getPlayerTransactions(player.getUniqueId());
        transactions.sort((a, b) -> Long.compare(b.getTime(), a.getTime()));
    }

    private void createInventory() {
        int totalPages = (int) Math.ceil(transactions.size() / 45.0);
        String title = plugin.getConfigManager().getGUI("transactions").getString("GUI-Title", "&b✹ &7Transactions (%page%/%max%)");
        title = title.replace("%page%", String.valueOf(page)).replace("%max%", String.valueOf(Math.max(1, totalPages)));
        title = plugin.getConfigManager().colorize(title);
        int size = plugin.getConfigManager().getGUI("transactions").getInt("size", 54);
        inventory = Bukkit.createInventory(null, size, title);
    }

    private void fillItems() {
        List<Integer> listingSlots = plugin.getConfigManager().getGUI("transactions").getIntegerList("Listing-Slots");
        int startIndex = (page - 1) * 45;
        for (int i = 0; i < listingSlots.size() && startIndex + i < transactions.size(); i++) {
            Transaction transaction = transactions.get(startIndex + i);
            ItemStack displayItem = createDisplayItem(transaction);
            inventory.setItem(listingSlots.get(i), displayItem);
        }
        setFiller();
        setControlItems();
    }

    private ItemStack createDisplayItem(Transaction transaction) {
        ItemStack item = transaction.getItem().clone();
        ItemMeta meta = item.getItemMeta();
        List<String> lore = plugin.getConfigManager().getGUI("transactions").getStringList("Transaction-Lore");
        List<String> coloredLore = new ArrayList<>();
        OfflinePlayer seller = Bukkit.getOfflinePlayer(transaction.getSeller());
        OfflinePlayer buyer = Bukkit.getOfflinePlayer(transaction.getBuyer());
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        for (String line : lore) {
            line = line.replace("%price%", plugin.getEconomyManager().format(transaction.getPrice()));
            line = line.replace("%seller%", seller.getName());
            line = line.replace("%customer%", buyer.getName());
            line = line.replace("%time%", sdf.format(new Date(transaction.getTime())));
            coloredLore.add(plugin.getConfigManager().colorize(line));
        }
        if (meta.hasLore() && meta.getLore() != null) {
            coloredLore.addAll(0, meta.getLore());
        }
        meta.setLore(coloredLore);
        item.setItemMeta(meta);
        return item;
    }

    private void setFiller() {
        String fillerName = plugin.getConfigManager().getGUI("transactions").getString("Filler.name", "");
        String fillerMaterial = plugin.getConfigManager().getGUI("transactions").getString("Filler.material", "BLACK_STAINED_GLASS_PANE");
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
        String backName = plugin.getConfigManager().getGUI("transactions").getString("Previous-Page-Item.name", "&e&lPREVIOUS PAGE");
        String backMaterial = plugin.getConfigManager().getGUI("transactions").getString("Previous-Page-Item.material", "ARROW");
        int backSlot = plugin.getConfigManager().getGUI("transactions").getInt("Previous-Page-Item.slot", 48);
        List<String> backLore = plugin.getConfigManager().getGUI("transactions").getStringList("Previous-Page-Item.lore");
        setItem(backSlot, backMaterial, backName, backLore);

        String backToAuctionName = plugin.getConfigManager().getGUI("transactions").getString("Back-Item.name", "&e&lGO BACK");
        String backToAuctionMaterial = plugin.getConfigManager().getGUI("transactions").getString("Back-Item.material", "RED_STAINED_GLASS_PANE");
        int backToAuctionSlot = plugin.getConfigManager().getGUI("transactions").getInt("Back-Item.slot", 49);
        List<String> backToAuctionLore = plugin.getConfigManager().getGUI("transactions").getStringList("Back-Item.lore");
        setItem(backToAuctionSlot, backToAuctionMaterial, backToAuctionName, backToAuctionLore);

        String nextName = plugin.getConfigManager().getGUI("transactions").getString("Next-Page-Item.name", "&e&lNEXT PAGE");
        String nextMaterial = plugin.getConfigManager().getGUI("transactions").getString("Next-Page-Item.material", "ARROW");
        int nextSlot = plugin.getConfigManager().getGUI("transactions").getInt("Next-Page-Item.slot", 50);
        List<String> nextLore = plugin.getConfigManager().getGUI("transactions").getStringList("Next-Page-Item.lore");
        setItem(nextSlot, nextMaterial, nextName, nextLore);

        String transactionName = plugin.getConfigManager().getGUI("transactions").getString("Transaction-Item.name", "&e&lAUCTION HISTORY");
        String transactionMaterial = plugin.getConfigManager().getGUI("transactions").getString("Transaction-Item.material", "WRITABLE_BOOK");
        int transactionSlot = plugin.getConfigManager().getGUI("transactions").getInt("Transaction-Item.slot", 53);
        List<String> transactionLore = plugin.getConfigManager().getGUI("transactions").getStringList("Transaction-Item.lore");
        double earned = plugin.getAuctionManager().getTotalEarned(player.getUniqueId());
        double spent = plugin.getAuctionManager().getTotalSpent(player.getUniqueId());
        List<String> coloredLore = new ArrayList<>();
        for (String line : transactionLore) {
            line = line.replace("%earned%", plugin.getEconomyManager().format(earned));
            line = line.replace("%spent%", plugin.getEconomyManager().format(spent));
            coloredLore.add(plugin.getConfigManager().colorize(line));
        }
        setItemWithLore(transactionSlot, transactionMaterial, transactionName, coloredLore);
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

    private void setItemWithLore(int slot, String materialName, String displayName, List<String> lore) {
        Material material = Material.getMaterial(materialName);
        if (material == null) material = Material.STONE;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(plugin.getConfigManager().colorize(displayName));
            if (lore != null && !lore.isEmpty()) {
                meta.setLore(lore);
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

        int backSlot = plugin.getConfigManager().getGUI("transactions").getInt("Previous-Page-Item.slot", 48);
        int nextSlot = plugin.getConfigManager().getGUI("transactions").getInt("Next-Page-Item.slot", 50);
        int backToAuctionSlot = plugin.getConfigManager().getGUI("transactions").getInt("Back-Item.slot", 49);
        int transactionSlot = plugin.getConfigManager().getGUI("transactions").getInt("Transaction-Item.slot", 53);

        if (slot == backSlot && page > 1) {
            playSound("page");
            new TransactionsGUI(plugin, player, page - 1).open();
            return;
        }
        if (slot == nextSlot) {
            int totalPages = (int) Math.ceil(transactions.size() / 45.0);
            if (page < totalPages) {
                playSound("page");
                new TransactionsGUI(plugin, player, page + 1).open();
            }
            return;
        }
        if (slot == backToAuctionSlot) {
            playSound("click");
            new AuctionGUI(plugin, player, 1, "Recently Listed").open();
            return;
        }
        if (slot == transactionSlot) {
            playSound("click");
            new TransactionsGUI(plugin, player, 1).open();
            return;
        }
    }
            }
