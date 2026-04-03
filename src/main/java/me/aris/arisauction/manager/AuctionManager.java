package me.aris.arisauction.manager;

import me.aris.arisauction.ArisAuction;
import me.aris.arisauction.model.AuctionItem;
import me.aris.arisauction.model.Transaction;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class AuctionManager {
    private final ArisAuction plugin;
    private final Map<UUID, AuctionItem> auctionItems;
    private final Map<UUID, List<Transaction>> playerTransactions;
    private final Map<UUID, Long> itemExpiry;
    private File dataFile;
    private YamlConfiguration dataConfig;
    private Player sortingPlayer;

    public AuctionManager(ArisAuction plugin) {
        this.plugin = plugin;
        this.auctionItems = new ConcurrentHashMap<>();
        this.playerTransactions = new ConcurrentHashMap<>();
        this.itemExpiry = new ConcurrentHashMap<>();
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
        loadData();
        startExpiryTask();
    }

    private void loadData() {
        if (!dataFile.exists()) {
            dataConfig = new YamlConfiguration();
            return;
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        
        if (dataConfig.contains("auctions")) {
            for (String key : dataConfig.getConfigurationSection("auctions").getKeys(false)) {
                UUID uuid = UUID.fromString(key);
                AuctionItem item = (AuctionItem) dataConfig.get("auctions." + key);
                if (item != null) {
                    auctionItems.put(uuid, item);
                    itemExpiry.put(uuid, item.getExpiryTime());
                }
            }
        }
        
        if (dataConfig.contains("transactions")) {
            for (String key : dataConfig.getConfigurationSection("transactions").getKeys(false)) {
                UUID playerUUID = UUID.fromString(key);
                List<?> list = dataConfig.getList("transactions." + key);
                if (list != null) {
                    List<Transaction> transactions = new ArrayList<>();
                    for (Object obj : list) {
                        if (obj instanceof Transaction) {
                            transactions.add((Transaction) obj);
                        }
                    }
                    playerTransactions.put(playerUUID, transactions);
                }
            }
        }
    }

    public void saveAll() {
        dataConfig.set("auctions", null);
        dataConfig.set("transactions", null);
        
        for (Map.Entry<UUID, AuctionItem> entry : auctionItems.entrySet()) {
            dataConfig.set("auctions." + entry.getKey().toString(), entry.getValue());
        }
        
        for (Map.Entry<UUID, List<Transaction>> entry : playerTransactions.entrySet()) {
            dataConfig.set("transactions." + entry.getKey().toString(), entry.getValue());
        }
        
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Cannot save auction data!");
        }
    }

    private void startExpiryTask() {
        if (plugin.isFolia()) {
            startFoliaExpiryTask();
        } else {
            startBukkitExpiryTask();
        }
    }

    private void startBukkitExpiryTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            checkExpiredItems();
        }, 0L, 20L);
    }

    private void startFoliaExpiryTask() {
        plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(plugin, (scheduledTask) -> {
            checkExpiredItems();
        }, 1L, 20L);
    }

    private void checkExpiredItems() {
        long currentTime = System.currentTimeMillis();
        List<UUID> toRemove = new ArrayList<>();
        
        for (Map.Entry<UUID, Long> entry : itemExpiry.entrySet()) {
            if (entry.getValue() <= currentTime) {
                toRemove.add(entry.getKey());
            }
        }
        
        for (UUID uuid : toRemove) {
            AuctionItem item = auctionItems.get(uuid);
            if (item != null) {
                Player seller = Bukkit.getPlayer(item.getSeller());
                if (seller != null && seller.isOnline()) {
                    seller.getInventory().addItem(item.getItemStack());
                    seller.sendMessage(plugin.getConfigManager().getMessage("item-expired"));
                }
                auctionItems.remove(uuid);
                itemExpiry.remove(uuid);
            }
        }
    }

    public void sellItem(Player player, ItemStack item, double price) {
        double minPrice = plugin.getConfigManager().getConfig().getDouble("min-sell-price", 1.0);
        double maxPrice = plugin.getConfigManager().getConfig().getDouble("max-sell-price", 1000000000.0);
        long expirySeconds = plugin.getConfigManager().getConfig().getLong("item-expiry-time", 86400);
        
        if (price < minPrice) {
            player.sendMessage(plugin.getConfigManager().getMessage("price-too-low").replace("%min%", String.valueOf(minPrice)));
            return;
        }
        
        if (price > maxPrice) {
            player.sendMessage(plugin.getConfigManager().getMessage("price-too-high").replace("%max%", String.valueOf(maxPrice)));
            return;
        }
        
        long expiryTime = System.currentTimeMillis() + (expirySeconds * 1000);
        
        AuctionItem auctionItem = new AuctionItem(UUID.randomUUID(), player.getUniqueId(), item.clone(), price, System.currentTimeMillis(), expiryTime);
        auctionItems.put(auctionItem.getId(), auctionItem);
        itemExpiry.put(auctionItem.getId(), expiryTime);
        player.getInventory().setItemInMainHand(null);
        player.sendMessage(plugin.getConfigManager().getMessage("item-listed").replace("%price%", plugin.getEconomyManager().format(price)));
    }

    public void buyItem(Player player, UUID itemId) {
        AuctionItem item = auctionItems.get(itemId);
        if (item == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("item-not-found"));
            return;
        }
        
        if (player.getUniqueId().equals(item.getSeller())) {
            player.sendMessage(plugin.getConfigManager().getMessage("cannot-buy-own"));
            return;
        }
        
        if (!plugin.getEconomyManager().hasEnough(player, item.getPrice())) {
            player.sendMessage(plugin.getConfigManager().getMessage("not-enough-money"));
            return;
        }
        
        Player seller = Bukkit.getPlayer(item.getSeller());
        
        plugin.getEconomyManager().withdraw(player, item.getPrice());
        if (seller != null && seller.isOnline()) {
            plugin.getEconomyManager().deposit(seller, item.getPrice());
        }
        
        player.getInventory().addItem(item.getItemStack());
        
        Transaction transaction = new Transaction(item.getItemStack(), item.getSeller(), player.getUniqueId(), item.getPrice(), System.currentTimeMillis());
        
        List<Transaction> sellerTrans = playerTransactions.getOrDefault(item.getSeller(), new ArrayList<>());
        sellerTrans.add(transaction);
        playerTransactions.put(item.getSeller(), sellerTrans);
        
        List<Transaction> buyerTrans = playerTransactions.getOrDefault(player.getUniqueId(), new ArrayList<>());
        buyerTrans.add(transaction);
        playerTransactions.put(player.getUniqueId(), buyerTrans);
        
        auctionItems.remove(itemId);
        itemExpiry.remove(itemId);
        
        player.sendMessage(plugin.getConfigManager().getMessage("purchase-success").replace("%price%", plugin.getEconomyManager().format(item.getPrice())));
        if (seller != null && seller.isOnline()) {
            seller.sendMessage(plugin.getConfigManager().getMessage("item-sold").replace("%buyer%", player.getName()).replace("%price%", plugin.getEconomyManager().format(item.getPrice())));
        }
    }

    public void cancelItem(Player player, UUID itemId) {
        AuctionItem item = auctionItems.get(itemId);
        if (item == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("item-not-found"));
            return;
        }
        
        if (!item.getSeller().equals(player.getUniqueId())) {
            player.sendMessage(plugin.getConfigManager().getMessage("not-your-item"));
            return;
        }
        
        player.getInventory().addItem(item.getItemStack());
        auctionItems.remove(itemId);
        itemExpiry.remove(itemId);
        player.sendMessage(plugin.getConfigManager().getMessage("item-cancelled"));
    }

    public void adminRemoveItem(UUID itemId) {
        AuctionItem item = auctionItems.get(itemId);
        if (item != null) {
            auctionItems.remove(itemId);
            itemExpiry.remove(itemId);
        }
    }

    public List<AuctionItem> getAuctions() {
        return new ArrayList<>(auctionItems.values());
    }

    public List<AuctionItem> searchItems(String searchTerm) {
        List<AuctionItem> results = new ArrayList<>();
        for (AuctionItem item : auctionItems.values()) {
            String itemName = item.getItemStack().getType().toString().toLowerCase();
            if (itemName.contains(searchTerm.toLowerCase())) {
                results.add(item);
            }
        }
        return results;
    }

    public List<AuctionItem> getAuctionsByPlayer(UUID playerUUID) {
        List<AuctionItem> items = new ArrayList<>();
        for (AuctionItem item : auctionItems.values()) {
            if (item.getSeller().equals(playerUUID)) {
                items.add(item);
            }
        }
        return items;
    }

    public List<Transaction> getPlayerTransactions(UUID playerUUID) {
        return playerTransactions.getOrDefault(playerUUID, new ArrayList<>());
    }

    public double getTotalEarned(UUID playerUUID) {
        double total = 0;
        for (Transaction trans : getPlayerTransactions(playerUUID)) {
            if (trans.getSeller().equals(playerUUID)) {
                total += trans.getPrice();
            }
        }
        return total;
    }

    public double getTotalSpent(UUID playerUUID) {
        double total = 0;
        for (Transaction trans : getPlayerTransactions(playerUUID)) {
            if (trans.getBuyer().equals(playerUUID)) {
                total += trans.getPrice();
            }
        }
        return total;
    }

    public void setSortingPlayer(Player player) {
        this.sortingPlayer = player;
        if (plugin.isFolia()) {
            plugin.getServer().getGlobalRegionScheduler().runDelayed(plugin, (task) -> {
                if (sortingPlayer != null && sortingPlayer.equals(player)) {
                    sortingPlayer = null;
                }
            }, 100L);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (sortingPlayer != null && sortingPlayer.equals(player)) {
                    sortingPlayer = null;
                }
            }, 100L);
        }
    }

    public Player getSortingPlayer() {
        return sortingPlayer;
    }

    public void handleSortCommand(Player player, String[] args) {
        if (sortingPlayer != null && sortingPlayer.equals(player)) {
            if (args.length >= 1) {
                String option = args[0];
                switch (option) {
                    case "1":
                        new me.aris.arisauction.gui.AuctionGUI(plugin, player, 1, "Recently Listed").open();
                        sortingPlayer = null;
                        break;
                    case "2":
                        new me.aris.arisauction.gui.AuctionGUI(plugin, player, 1, "Last Listed").open();
                        sortingPlayer = null;
                        break;
                    case "3":
                        new me.aris.arisauction.gui.AuctionGUI(plugin, player, 1, "Lowest Price").open();
                        sortingPlayer = null;
                        break;
                    case "4":
                        new me.aris.arisauction.gui.AuctionGUI(plugin, player, 1, "Highest Price").open();
                        sortingPlayer = null;
                        break;
                    default:
                        player.sendMessage(plugin.getConfigManager().colorize("&cInvalid option! Use &61-4"));
                        break;
                }
            } else {
                player.sendMessage(plugin.getConfigManager().colorize("&cUsage: &6/sort <1-4>"));
            }
        } else {
            player.sendMessage(plugin.getConfigManager().colorize("&cYou are not in sort mode! Use &6/ah &cto open auction first."));
        }
    }
    }
