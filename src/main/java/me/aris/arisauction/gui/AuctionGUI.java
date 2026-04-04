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
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class AuctionGUI {
    private final ArisAuction plugin;
    private final Player player;
    private int page;
    private String sortType;
    private String searchTerm;
    private List<AuctionItem> items;
    private Inventory inventory;

    public AuctionGUI(ArisAuction plugin, Player player, int page, String sortType) {
        this(plugin, player, page, sortType, null);
    }

    public AuctionGUI(ArisAuction plugin, Player player, int page, String sortType, String searchTerm) {
        this.plugin = plugin;
        this.player = player;
        this.page = page;
        this.sortType = sortType;
        this.searchTerm = searchTerm;
        this.items = new ArrayList<>();
    }

    public void open() {
        loadItems();
        sortItems();
        createInventory();
        fillItems();
        plugin.getServer().getPluginManager().registerEvents(plugin.getAuctionListener(), plugin);
        plugin.getAuctionListener().registerAuctionGUI(player, this);
        player.openInventory(inventory);
    }

    private void loadItems() {
        if (searchTerm != null && !searchTerm.isEmpty()) {
            items = plugin.getAuctionManager().searchItems(searchTerm);
        } else {
            items = plugin.getAuctionManager().getAuctions();
        }
    }

    private void sortItems() {
        switch (sortType) {
            case "Lowest Price":
                items.sort(Comparator.comparingDouble(AuctionItem::getPrice));
                break;
            case "Highest Price":
                items.sort((a, b) -> Double.compare(b.getPrice(), a.getPrice()));
                break;
            case "Last Listed":
                items.sort((a, b) -> Long.compare(b.getListedTime(), a.getListedTime()));
                break;
            case "Recently Listed":
            default:
                items.sort((a, b) -> Long.compare(b.getListedTime(), a.getListedTime()));
                break;
        }
    }

    private void createInventory() {
        String title = plugin.getConfigManager().getGUI("auction").getString("GUI-Title", "&8AUCTION (Page %page%)");
        title = title.replace("%page%", String.valueOf(page));
        if (searchTerm != null && !searchTerm.isEmpty()) {
            title = title + " &7- &fSearch: " + searchTerm;
        }
        title = plugin.getConfigManager().colorize(title);
        int size = plugin.getConfigManager().getGUI("auction").getInt("size", 54);
        inventory = Bukkit.createInventory(null, size, title);
    }

    private void fillItems() {
        List<Integer> listingSlots = plugin.getConfigManager().getGUI("auction").getIntegerList("Listing-Slots");
        for (int i = 0; i < listingSlots.size() && i < items.size(); i++) {
            AuctionItem auctionItem = items.get(i);
            ItemStack displayItem = createDisplayItem(auctionItem);
            inventory.setItem(listingSlots.get(i), displayItem);
        }
        setFiller();
        setControlItems();
    }

    private ItemStack createDisplayItem(AuctionItem auctionItem) {
        ItemStack item = auctionItem.getItemStack().clone();
        ItemMeta meta = item.getItemMeta();
        List<String> lore;
        if (player.hasPermission("arisauction.admin")) {
            lore = plugin.getConfigManager().getGUI("auction").getStringList("Product-Lore-Admin");
        } else {
            lore = plugin.getConfigManager().getGUI("auction").getStringList("Product-Lore");
        }
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
        String fillerName = plugin.getConfigManager().getGUI("auction").getString("Filler.name", "");
        String fillerMaterial = plugin.getConfigManager().getGUI("auction").getString("Filler.material", "BLACK_STAINED_GLASS_PANE");
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
        String searchName = plugin.getConfigManager().getGUI("auction").getString("Search-Item.name", "#28f886SEARCH");
        String searchMaterial = plugin.getConfigManager().getGUI("auction").getString("Search-Item.material", "OAK_SIGN");
        int searchSlot = plugin.getConfigManager().getGUI("auction").getInt("Search-Item.slot", 45);
        List<String> searchLore = plugin.getConfigManager().getGUI("auction").getStringList("Search-Item.lore");
        setItem(searchSlot, searchMaterial, searchName, searchLore);

        String sortName = plugin.getConfigManager().getGUI("auction").getString("Sorting-Item.name", "#00fc88SORT");
        String sortMaterial = plugin.getConfigManager().getGUI("auction").getString("Sorting-Item.material", "CAULDRON");
        int sortSlot = plugin.getConfigManager().getGUI("auction").getInt("Sorting-Item.slot", 46);
        List<String> sortLore = plugin.getConfigManager().getGUI("auction").getStringList("Sorting-Item.lore");
        List<String> coloredSortLore = new ArrayList<>();
        String sortColor1 = sortType.equals("Recently Listed") ? "&a" : "&7";
        String sortColor2 = sortType.equals("Last Listed") ? "&a" : "&7";
        String sortColor3 = sortType.equals("Lowest Price") ? "&a" : "&7";
        String sortColor4 = sortType.equals("Highest Price") ? "&a" : "&7";
        for (String line : sortLore) {
            line = line.replace("%sort_color_1%", plugin.getConfigManager().colorize(sortColor1));
            line = line.replace("%sort_color_2%", plugin.getConfigManager().colorize(sortColor2));
            line = line.replace("%sort_color_3%", plugin.getConfigManager().colorize(sortColor3));
            line = line.replace("%sort_color_4%", plugin.getConfigManager().colorize(sortColor4));
            coloredSortLore.add(plugin.getConfigManager().colorize(line));
        }
        setItemWithLore(sortSlot, sortMaterial, sortName, coloredSortLore);

        String backName = plugin.getConfigManager().getGUI("auction").getString("Previous-Page-Item.name", "#28f886BACK");
        String backMaterial = plugin.getConfigManager().getGUI("auction").getString("Previous-Page-Item.material", "ARROW");
        int backSlot = plugin.getConfigManager().getGUI("auction").getInt("Previous-Page-Item.slot", 48);
        List<String> backLore = plugin.getConfigManager().getGUI("auction").getStringList("Previous-Page-Item.lore");
        setItem(backSlot, backMaterial, backName, backLore);

        String refreshName = plugin.getConfigManager().getGUI("auction").getString("Refresh-Item.name", "#28f886AUCTION");
        String refreshMaterial = plugin.getConfigManager().getGUI("auction").getString("Refresh-Item.material", "ANVIL");
        int refreshSlot = plugin.getConfigManager().getGUI("auction").getInt("Refresh-Item.slot", 49);
        List<String> refreshLore = plugin.getConfigManager().getGUI("auction").getStringList("Refresh-Item.lore");
        setItem(refreshSlot, refreshMaterial, refreshName, refreshLore);

        String nextName = plugin.getConfigManager().getGUI("auction").getString("Next-Page-Item.name", "#28f886NEXT");
        String nextMaterial = plugin.getConfigManager().getGUI("auction").getString("Next-Page-Item.material", "ARROW");
        int nextSlot = plugin.getConfigManager().getGUI("auction").getInt("Next-Page-Item.slot", 50);
        List<String> nextLore = plugin.getConfigManager().getGUI("auction").getStringList("Next-Page-Item.lore");
        setItem(nextSlot, nextMaterial, nextName, nextLore);

        String yourItemsName = plugin.getConfigManager().getGUI("auction").getString("Your-Items-Item.name", "#28f886YOUR ITEMS");
        String yourItemsMaterial = plugin.getConfigManager().getGUI("auction").getString("Your-Items-Item.material", "CHEST");
        int yourItemsSlot = plugin.getConfigManager().getGUI("auction").getInt("Your-Items-Item.slot", 52);
        List<String> yourItemsLore = plugin.getConfigManager().getGUI("auction").getStringList("Your-Items-Item.lore");
        setItem(yourItemsSlot, yourItemsMaterial, yourItemsName, yourItemsLore);

        String transactionName = plugin.getConfigManager().getGUI("auction").getString("Transaction-Item.name", "#28f886TRANSACTIONS");
        String transactionMaterial = plugin.getConfigManager().getGUI("auction").getString("Transaction-Item.material", "WRITABLE_BOOK");
        int transactionSlot = plugin.getConfigManager().getGUI("auction").getInt("Transaction-Item.slot", 53);
        List<String> transactionLore = plugin.getConfigManager().getGUI("auction").getStringList("Transaction-Item.lore");
        setItem(transactionSlot, transactionMaterial, transactionName, transactionLore);
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

    public void handleClick(InventoryClickEvent event) {
        if (inventory == null) return;
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) return;

        int backSlot = plugin.getConfigManager().getGUI("auction").getInt("Previous-Page-Item.slot", 48);
        int nextSlot = plugin.getConfigManager().getGUI("auction").getInt("Next-Page-Item.slot", 50);
        int refreshSlot = plugin.getConfigManager().getGUI("auction").getInt("Refresh-Item.slot", 49);
        int searchSlot = plugin.getConfigManager().getGUI("auction").getInt("Search-Item.slot", 45);
        int sortSlot = plugin.getConfigManager().getGUI("auction").getInt("Sorting-Item.slot", 46);
        int yourItemsSlot = plugin.getConfigManager().getGUI("auction").getInt("Your-Items-Item.slot", 52);
        int transactionSlot = plugin.getConfigManager().getGUI("auction").getInt("Transaction-Item.slot", 53);

        if (slot == backSlot && page > 1) {
            playSound("page");
            player.closeInventory();
            new AuctionGUI(plugin, player, page - 1, sortType, searchTerm).open();
            return;
        }
        if (slot == nextSlot) {
            int totalPages = (int) Math.ceil(items.size() / 45.0);
            if (page < totalPages) {
                playSound("page");
                player.closeInventory();
                new AuctionGUI(plugin, player, page + 1, sortType, searchTerm).open();
            }
            return;
        }
        if (slot == refreshSlot) {
            playSound("refresh");
            player.closeInventory();
            new AuctionGUI(plugin, player, page, sortType, searchTerm).open();
            return;
        }
        if (slot == searchSlot) {
            playSound("search");
            player.closeInventory();
            player.sendMessage(plugin.getConfigManager().getMessage("search-usage"));
            return;
        }
        if (slot == sortSlot) {
            playSound("sort");
            player.closeInventory();
            openSortMenu();
            return;
        }
        if (slot == yourItemsSlot) {
            playSound("click");
            player.closeInventory();
            new YourItemsGUI(plugin, player, 1).open();
            return;
        }
        if (slot == transactionSlot) {
            playSound("click");
            player.closeInventory();
            new TransactionsGUI(plugin, player, 1).open();
            return;
        }

        List<Integer> listingSlots = plugin.getConfigManager().getGUI("auction").getIntegerList("Listing-Slots");
        int itemIndex = listingSlots.indexOf(slot);
        if (itemIndex >= 0 && itemIndex < items.size()) {
            AuctionItem auctionItem = items.get(itemIndex);
            if (event.isLeftClick()) {
                playSound("buy");
                plugin.getAuctionManager().buyItem(player, auctionItem.getId());
                player.closeInventory();
                new AuctionGUI(plugin, player, page, sortType, searchTerm).open();
            } else if (event.isRightClick()) {
                playSound("click");
                player.sendMessage(plugin.getConfigManager().getMessage("report-sent"));
            } else if (event.isShiftClick() && player.hasPermission("arisauction.admin")) {
                playSound("click");
                plugin.getAuctionManager().adminRemoveItem(auctionItem.getId());
                player.sendMessage(plugin.getConfigManager().getMessage("admin-remove").replace("%item%", auctionItem.getItemStack().getType().toString()));
                player.closeInventory();
                new AuctionGUI(plugin, player, page, sortType, searchTerm).open();
            }
        }
    }

    private void openSortMenu() {
        new BukkitRunnable() {
            @Override
            public void run() {
                player.sendMessage(plugin.getConfigManager().colorize("&6&l=== SORT OPTIONS ==="));
                player.sendMessage(plugin.getConfigManager().colorize("&a1. &fRecently Listed"));
                player.sendMessage(plugin.getConfigManager().colorize("&a2. &fLast Listed"));
                player.sendMessage(plugin.getConfigManager().colorize("&a3. &fLowest Price"));
                player.sendMessage(plugin.getConfigManager().colorize("&a4. &fHighest Price"));
                player.sendMessage(plugin.getConfigManager().colorize("&6&l==================="));
                player.sendMessage(plugin.getConfigManager().colorize("&eType &6/sort <number> &eto choose"));
                plugin.getAuctionManager().setSortingPlayer(player);
            }
        }.runTaskLater(plugin, 1L);
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
    }
