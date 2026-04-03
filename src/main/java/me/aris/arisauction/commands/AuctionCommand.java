package me.aris.arisauction.commands;

import me.aris.arisauction.ArisAuction;
import me.aris.arisauction.gui.AuctionGUI;
import me.aris.arisauction.gui.ConfirmSellGUI;
import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AuctionCommand implements CommandExecutor, TabCompleter {
    private final ArisAuction plugin;
    public AuctionCommand(ArisAuction plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("only-player"));
            return true;
        }
        Player p = (Player) sender;
        if (args.length == 0) {
            new AuctionGUI(plugin, p, 1, "Recently Listed").open();
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "sell":
                if (args.length < 2) {
                    p.sendMessage(plugin.getConfigManager().getMessage("sell-usage"));
                    return true;
                }
                double price;
                try { price = Double.parseDouble(args[1]); }
                catch (NumberFormatException e) {
                    p.sendMessage(plugin.getConfigManager().getMessage("invalid-price"));
                    return true;
                }
                ItemStack item = p.getInventory().getItemInMainHand();
                if (item == null || item.getType() == Material.AIR) {
                    p.sendMessage(plugin.getConfigManager().getMessage("no-item-in-hand"));
                    return true;
                }
                new ConfirmSellGUI(plugin, p, item, price).open();
                break;
            case "search":
                if (args.length < 2) {
                    p.sendMessage(plugin.getConfigManager().getMessage("search-usage"));
                    return true;
                }
                String search = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                new AuctionGUI(plugin, p, 1, "Recently Listed", search).open();
                break;
            case "reload":
                if (!p.hasPermission("arisauction.admin")) {
                    p.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                    return true;
                }
                plugin.getConfigManager().reloadAll();
                p.sendMessage(plugin.getConfigManager().getMessage("reload-success"));
                break;
            default:
                p.sendMessage(plugin.getConfigManager().getMessage("unknown-command"));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            list.add("sell");
            list.add("search");
            if (sender.hasPermission("arisauction.admin")) list.add("reload");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("sell")) {
            list.add("<price>");
        } else if (args.length >= 2 && args[0].equalsIgnoreCase("search")) {
            list.add("<item name>");
        }
        return list;
    }
                               }
