package me.aris.arisauction.listener;

import me.aris.arisauction.ArisAuction;
import me.aris.arisauction.gui.AuctionGUI;
import me.aris.arisauction.gui.ConfirmSellGUI;
import me.aris.arisauction.gui.TransactionsGUI;
import me.aris.arisauction.gui.YourItemsGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class AuctionListener implements Listener {
    private final ArisAuction plugin;

    public AuctionListener(ArisAuction plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        String title = event.getView().getTitle();
        
        if (title.contains("AUCTION") || title.contains("My Items") || title.contains("Transactions") || title.contains("CONFIRM")) {
            event.setCancelled(true);
            
            Player player = (Player) event.getWhoClicked();
            
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
                ConfirmSellGUI gui = new ConfirmSellGUI(plugin, player, null, 0);
                gui.handleClick(event);
            }
        }
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage().toLowerCase();
        
        if (message.startsWith("/sort ")) {
            event.setCancelled(true);
            String[] args = message.substring(6).split(" ");
            plugin.getAuctionManager().handleSortCommand(player, args);
        } else if (message.equals("/sort")) {
            event.setCancelled(true);
            plugin.getAuctionManager().handleSortCommand(player, new String[0]);
        }
    }
  }
