package me.aris.arisauction.commands;

import me.aris.arisauction.ArisAuction;
import me.aris.arisauction.gui.AuctionGUI;
import me.aris.arisauction.gui.ConfirmSellGUI;
import me.aris.arisauction.gui.TransactionsGUI;
import me.aris.arisauction.gui.YourItemsGUI;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AuctionCommand implements CommandExecutor, TabCompleter {
    private final ArisAuction plugin;

    public AuctionCommand(ArisAuction plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("only-player"));
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            new AuctionGUI(plugin, player, 1, "Recently Listed").open();
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "sell":
                if (args.length < 2) {
                    player.sendMessage(plugin.getConfigManager().getMessage("sell-usage"));
                    return true;
                }
                double price;
                try {
                    price = Double.parseDouble(args[1]);
                } catch (NumberFormatException e) {
                    player.sendMessage(plugin.getConfigManager().getMessage("invalid-price"));
                    return true;
                }
                ItemStack item = player.getInventory().getItemInMainHand();
                if (item == null || item.getType() == Material.AIR) {
                    player.sendMessage(plugin.getConfigManager().getMessage("no-item-in-hand"));
                    return true;
                }
                new ConfirmSellGUI(plugin, player, item, price).open();
                break;

            case "search":
                if (args.length < 2) {
                    player.sendMessage(plugin.getConfigManager().getMessage("search-usage"));
                    return true;
                }
                String searchTerm = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                new AuctionGUI(plugin, player, 1, "Recently Listed", searchTerm).open();
                break;

            case "reload":
                if (!player.hasPermission("arisauction.admin")) {
                    player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                    return true;
                }
                plugin.getConfigManager().reloadAll();
                player.sendMessage(plugin.getConfigManager().getMessage("reload-success"));
                break;

            case "myitems":
                new YourItemsGUI(plugin, player, 1).open();
                break;

            case "transactions":
                new TransactionsGUI(plugin, player, 1).open();
                break;

            default:
                player.sendMessage(plugin.getConfigManager().getMessage("unknown-command"));
                break;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.add("sell");
            completions.add("search");
            completions.add("myitems");
            completions.add("transactions");
            if (sender.hasPermission("arisauction.admin")) {
                completions.add("reload");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("sell")) {
            completions.add("<price>");
        } else if (args.length >= 2 && args[0].equalsIgnoreCase("search")) {
            completions.add("<item name>");
        }
        
        return completions;
    }
              }
