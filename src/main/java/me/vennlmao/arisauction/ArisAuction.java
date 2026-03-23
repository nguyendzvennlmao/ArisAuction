package me.vennlmao.arisauction;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArisAuction extends JavaPlugin implements Listener, CommandExecutor {

    private static Economy econ = null;
    private FileConfiguration guiCfg, msgCfg;
    private final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    @Override
    public void onEnable() {
        if (!setupEconomy()) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        loadFiles();
        getCommand("ah").setExecutor(this);
        getServer().getPluginManager().registerEvents(this, this);
    }

    private void loadFiles() {
        saveDefaultConfig();
        File guiFile = new File(getDataFolder(), "gui.yml");
        File msgFile = new File(getDataFolder(), "message.yml");
        if (!guiFile.exists()) saveResource("gui.yml", false);
        if (!msgFile.exists()) saveResource("message.yml", false);
        guiCfg = YamlConfiguration.loadConfiguration(guiFile);
        msgCfg = YamlConfiguration.loadConfiguration(msgFile);
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        econ = rsp.getProvider();
        return econ != null;
    }

    public String color(String msg) {
        if (msg == null) return "";
        Matcher matcher = HEX_PATTERN.matcher(msg);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(sb, ChatColor.of("#" + matcher.group(1)).toString());
        }
        return ChatColor.translateAlternateColorCodes('&', matcher.appendTail(sb).toString());
    }

    private void sendMessage(Player player, String path, String... replacements) {
        String raw = msgCfg.getString(path);
        if (raw == null) return;
        String prefix = msgCfg.getString("prefix", "");
        String message = raw.replace("%prefix%", prefix);
        for (int i = 0; i < replacements.length; i += 2) {
            message = message.replace(replacements[i], replacements[i+1]);
        }
        String colored = color(message);
        if (msgCfg.getBoolean("chat", true)) {
            player.sendMessage(colored);
        }
        if (msgCfg.getBoolean("actionbar", true)) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(colored));
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        if (args.length > 0 && args[0].equalsIgnoreCase("sell")) {
            if (args.length < 2) return false;
            handleSell(player, args[1]);
            return true;
        }
        openAuctionGUI(player);
        return true;
    }

    private void handleSell(Player player, String priceStr) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            sendMessage(player, "no-item-in-hand");
            return;
        }
        try {
            double price = Double.parseDouble(priceStr);
            sendMessage(player, "sell-success", "%item%", item.getType().name(), "%price%", priceStr);
            player.getInventory().setItemInMainHand(null);
        } catch (Exception e) {
            sendMessage(player, "invalid-price");
        }
    }

    public void openAuctionGUI(Player player) {
        String title = color(guiCfg.getString("menu-title").replace("%page%", "1"));
        Inventory inv = Bukkit.createInventory(null, 54, title);
        player.getScheduler().execute(this, () -> player.openInventory(inv), null, 0L);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getClickedInventory() == null) return;
        String configTitle = ChatColor.stripColor(color(guiCfg.getString("menu-title").split("\\|")[0].trim()));
        if (ChatColor.stripColor(event.getView().getTitle()).contains(configTitle)) {
            event.setCancelled(true);
        }
    }
}
