package me.aris.arisauction.manager;

import me.aris.arisauction.ArisAuction;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;

public class EconomyManager {
    private final ArisAuction plugin;
    private final DecimalFormat decimalFormat;

    public EconomyManager(ArisAuction plugin) {
        this.plugin = plugin;
        this.decimalFormat = new DecimalFormat("#,##0.00");
    }

    public boolean hasEnough(Player player, double amount) {
        return plugin.getEconomy().has(player, amount);
    }

    public void withdraw(Player player, double amount) {
        plugin.getEconomy().withdrawPlayer(player, amount);
    }

    public void deposit(Player player, double amount) {
        plugin.getEconomy().depositPlayer(player, amount);
    }

    public double getBalance(Player player) {
        return plugin.getEconomy().getBalance(player);
    }

    public String format(double amount) {
        String k = plugin.getConfigManager().getConfig().getString("amount-format.k", "K");
        String m = plugin.getConfigManager().getConfig().getString("amount-format.m", "M");
        String b = plugin.getConfigManager().getConfig().getString("amount-format.b", "B");
        String t = plugin.getConfigManager().getConfig().getString("amount-format.t", "T");
        
        if (amount >= 1_000_000_000_000.0) {
            return String.format("%.2f" + t, amount / 1_000_000_000_000.0);
        } else if (amount >= 1_000_000_000.0) {
            return String.format("%.2f" + b, amount / 1_000_000_000.0);
        } else if (amount >= 1_000_000.0) {
            return String.format("%.2f" + m, amount / 1_000_000.0);
        } else if (amount >= 1_000.0) {
            return String.format("%.2f" + k, amount / 1_000.0);
        }
        return decimalFormat.format(amount);
    }
                                 }
