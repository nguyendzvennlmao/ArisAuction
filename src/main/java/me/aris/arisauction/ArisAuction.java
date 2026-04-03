package me.aris.arisauction;

import me.aris.arisauction.commands.AuctionCommand;
import me.aris.arisauction.listener.AuctionListener;
import me.aris.arisauction.manager.AuctionManager;
import me.aris.arisauction.manager.ConfigManager;
import me.aris.arisauction.manager.EconomyManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class ArisAuction extends JavaPlugin {
    private static ArisAuction instance;
    private ConfigManager configManager;
    private AuctionManager auctionManager;
    private EconomyManager economyManager;
    private Economy economy;

    @Override
    public void onEnable() {
        instance = this;
        
        if (!setupEconomy()) {
            getLogger().severe("Vault economy not found! Disabling plugin...");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        
        saveDefaultConfig();
        configManager = new ConfigManager(this);
        configManager.loadAllConfigs();
        economyManager = new EconomyManager(this);
        auctionManager = new AuctionManager(this);
        
        getCommand("auction").setExecutor(new AuctionCommand(this));
        getCommand("auc").setExecutor(new AuctionCommand(this));
        getCommand("ah").setExecutor(new AuctionCommand(this));
        Bukkit.getPluginManager().registerEvents(new AuctionListener(this), this);
        
        getLogger().info("ArisAuction has been enabled! (Folia Support: " + isFolia() + ")");
    }

    private boolean setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    @Override
    public void onDisable() {
        if (auctionManager != null) {
            auctionManager.saveAll();
        }
        getLogger().info("ArisAuction has been disabled!");
    }

    public static ArisAuction getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public AuctionManager getAuctionManager() {
        return auctionManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public Economy getEconomy() {
        return economy;
    }

    public boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
  }
