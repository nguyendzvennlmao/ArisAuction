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
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.Inventory;

public class AuctionListener implements Listener {
    private final ArisAuction plugin;

    public AuctionListener(ArisAuction plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Inventory inventory = event.getInventory();
        if (inventory == null) return;
        
        String title = event.getView().getTitle();
        
        if (title.contains("AUCTION") || title.contains("My Items") || title.contains("Transactions") || title.contains("CONFIRM")) {
            event.setCancelled(true);
            event.setResult(org.bukkit.event.Event.Result.DENY);
            
            Player player = (Player) event.getWhoClicked();
            
            if (event.getSlotType() == InventoryType.SlotType.OUTSIDE) {
                return;
            }
            
            if (title.contains("AUCTION")) {
                AuctionGUI gui = new AuctionGUI(plugin, player, 1, "Recently Listed");
                gui.handleClick(event);
            } else if (title.contains("My Items")) {
                YourItemsGUI gui = new YourItemsGUI(plugin, player, 1);
                gui.handleClick(event);
            } else if (title.contains("Transactions")) {
                TransactionsGUI gui = new TransactionsGUI(plugin, player, 1);
                gui.handleClick(event);
            } else if (title.contains("CONFIRM")) {
                if (event.getRawSlot() == 11 || event.getRawSlot() == 15) {
                    ConfirmSellGUI gui = new ConfirmSellGUI(plugin, player, null, 0);
                    gui.handleClick(event);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        String title = event.getView().getTitle();
        if (title.contains("AUCTION") || title.contains("My Items") || title.contains("Transactions") || title.contains("CONFIRM")) {
            event.setCancelled(true);
            event.setResult(org.bukkit.event.Event.Result.DENY);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        if (player.getOpenInventory() != null) {
            String title = player.getOpenInventory().getTitle();
            if (title.contains("AUCTION") || title.contains("My Items") || title.contains("Transactions") || title.contains("CONFIRM")) {
                event.setCancelled(true);
            }
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
