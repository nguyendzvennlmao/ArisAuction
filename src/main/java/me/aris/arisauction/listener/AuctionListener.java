package me.aris.arisauction.listener;

import me.aris.arisauction.ArisAuction;
import me.aris.arisauction.gui.AuctionGUI;
import me.aris.arisauction.gui.ConfirmSellGUI;
import me.aris.arisauction.gui.TransactionsGUI;
import me.aris.arisauction.gui.YourItemsGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AuctionListener implements Listener {
    private final ArisAuction plugin;
    private final Map<UUID, AuctionGUI> openAuctionGUIs;
    private final Map<UUID, YourItemsGUI> openYourItemsGUIs;
    private final Map<UUID, TransactionsGUI> openTransactionsGUIs;
    private final Map<UUID, ConfirmSellGUI> openConfirmSellGUIs;

    public AuctionListener(ArisAuction plugin) {
        this.plugin = plugin;
        this.openAuctionGUIs = new HashMap<>();
        this.openYourItemsGUIs = new HashMap<>();
        this.openTransactionsGUIs = new HashMap<>();
        this.openConfirmSellGUIs = new HashMap<>();
    }

    public void registerAuctionGUI(Player player, AuctionGUI gui) {
        openAuctionGUIs.put(player.getUniqueId(), gui);
    }

    public void registerYourItemsGUI(Player player, YourItemsGUI gui) {
        openYourItemsGUIs.put(player.getUniqueId(), gui);
    }

    public void registerTransactionsGUI(Player player, TransactionsGUI gui) {
        openTransactionsGUIs.put(player.getUniqueId(), gui);
    }

    public void registerConfirmSellGUI(Player player, ConfirmSellGUI gui) {
        openConfirmSellGUIs.put(player.getUniqueId(), gui);
    }

    public void unregisterGUI(Player player) {
        openAuctionGUIs.remove(player.getUniqueId());
        openYourItemsGUIs.remove(player.getUniqueId());
        openTransactionsGUIs.remove(player.getUniqueId());
        openConfirmSellGUIs.remove(player.getUniqueId());
    }

    private boolean isAuctionGui(String title) {
        return title.contains("AUCTION") || title.contains("My Items") || title.contains("Transactions") || title.contains("CONFIRM");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        if (isAuctionGui(title)) {
            event.setCancelled(true);
            event.setResult(org.bukkit.event.Event.Result.DENY);

            if (event.getCursor() != null && !event.getCursor().getType().isAir()) {
                player.setItemOnCursor(null);
            }

            UUID uuid = player.getUniqueId();

            if (title.contains("AUCTION") && openAuctionGUIs.containsKey(uuid)) {
                openAuctionGUIs.get(uuid).handleClick(event);
            } else if (title.contains("My Items") && openYourItemsGUIs.containsKey(uuid)) {
                openYourItemsGUIs.get(uuid).handleClick(event);
            } else if (title.contains("Transactions") && openTransactionsGUIs.containsKey(uuid)) {
                openTransactionsGUIs.get(uuid).handleClick(event);
            } else if (title.contains("CONFIRM") && openConfirmSellGUIs.containsKey(uuid)) {
                openConfirmSellGUIs.get(uuid).handleClick(event);
            }

            new BukkitRunnable() {
                @Override
                public void run() {
                    player.updateInventory();
                }
            }.runTask(plugin);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (isAuctionGui(event.getView().getTitle())) {
            event.setCancelled(true);
            event.setResult(org.bukkit.event.Event.Result.DENY);
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (player.getOpenInventory() != null && isAuctionGui(player.getOpenInventory().getTitle())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        if (player.getOpenInventory() != null && isAuctionGui(player.getOpenInventory().getTitle())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String msg = event.getMessage().toLowerCase();
        if (msg.startsWith("/sort ")) {
            event.setCancelled(true);
            plugin.getAuctionManager().handleSortCommand(player, msg.substring(6).split(" "));
        } else if (msg.equals("/sort")) {
            event.setCancelled(true);
            plugin.getAuctionManager().handleSortCommand(player, new String[0]);
        }
    }
            }
