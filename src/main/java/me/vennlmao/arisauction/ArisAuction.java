package me.vennlmao.arisauction;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
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
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArisAuction extends JavaPlugin implements Listener, CommandExecutor {

    private static Economy econ = null;
    private FileConfiguration guiCfg, msgCfg;
    private final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    @Override
    public void onEnable() {
        setupEconomy();
        loadFiles();
        getCommand("ah").setExecutor(this);
        getServer().getPluginManager().registerEvents(this, this);
    }

    private void loadFiles() {
        saveDefaultConfig();
        guiCfg = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "gui.yml"));
        msgCfg = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "message.yml"));
    }

    private void setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return;
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp != null) {
            econ = rsp.getProvider();
        }
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

    private void playSound(Player player, String type) {
        String soundName = guiCfg.getString("sounds." + type);
        if (soundName != null) {
            player.playSound(player.getLocation(), Sound.valueOf(soundName), 1.0f, 1.0f);
        }
    }

    private void sendMessage(Player player, String path, String item, String price) {
        String rawMsg = msgCfg.getString(path);
        if (rawMsg == null) return;
        
        String prefix = msgCfg.getString("prefix", "");
        String formatted = rawMsg.replace("%prefix%", prefix);
        if (item != null) formatted = formatted.replace("%item%", item);
        if (price != null) formatted = formatted.replace("%price%", price);
        
        String colored = color(formatted);
        if (msgCfg.getBoolean("chat")) player.sendMessage(colored);
        if (msgCfg.getBoolean("actionbar")) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(colored));
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        if (args.length > 0 && args[0].equalsIgnoreCase("sell")) {
            handleSell(player, args.length > 1 ? args[1] : "0");
            return true;
        }
        openMainGUI(player, "recently_listed", "all");
        return true;
    }

    private void handleSell(Player player, String price) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            playSound(player, "error");
            sendMessage(player, "no-item-in-hand", null, null);
            return;
        }
        playSound(player, "success");
        sendMessage(player, "sell-success", item.getType().name(), price);
        player.getInventory().setItemInMainHand(null);
    }

    public void openMainGUI(Player player, String sort, String filter) {
        playSound(player, "open-menu");
        Inventory inv = Bukkit.createInventory(null, 54, color(guiCfg.getString("menu-title").replace("%page%", "1")));
        inv.setItem(guiCfg.getInt("buttons.my-auctions.slot"), createSimpleItem("buttons.my-auctions"));
        inv.setItem(guiCfg.getInt("sort.slot"), createSelectionItem("sort", sort));
        inv.setItem(guiCfg.getInt("filter.slot"), createSelectionItem("filter", filter));
        player.getScheduler().execute(this, () -> player.openInventory(inv), null, 0L);
    }

    public void openConfirmGUI(Player player, ItemStack item) {
        playSound(player, "click");
        Inventory inv = Bukkit.createInventory(null, 27, color(guiCfg.getString("confirm-title")));
        inv.setItem(guiCfg.getInt("confirm-gui.cancel.slot"), createSimpleItem("confirm-gui.cancel"));
        inv.setItem(guiCfg.getInt("confirm-gui.accept.slot"), createSimpleItem("confirm-gui.accept"));
        inv.setItem(guiCfg.getInt("confirm-gui.info-slot"), item);
        player.getScheduler().execute(this, () -> player.openInventory(inv), null, 0L);
    }

    private ItemStack createSimpleItem(String path) {
        Material mat = Material.valueOf(guiCfg.getString(path + ".material", "STONE"));
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(guiCfg.getString(path + ".name")));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createSelectionItem(String type, String active) {
        Material mat = Material.valueOf(guiCfg.getString(type + ".material", "STONE"));
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(guiCfg.getString(type + ".name")));
            List<String> lore = new ArrayList<>();
            if (guiCfg.getConfigurationSection(type + ".options") != null) {
                for (String key : guiCfg.getConfigurationSection(type + ".options").getKeys(false)) {
                    String prefix = key.equals(active) ? guiCfg.getString(type + ".active_prefix") : guiCfg.getString(type + ".inactive_prefix");
                    lore.add(color(prefix + guiCfg.getString(type + ".options." + key)));
                }
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getClickedInventory() == null) return;
        Player p = (Player) event.getWhoClicked();
        String title = ChatColor.stripColor(event.getView().getTitle());

        if (title.contains("Auction House")) {
            event.setCancelled(true);
            if (event.getRawSlot() == guiCfg.getInt("buttons.my-auctions.slot")) {
                Inventory inv = Bukkit.createInventory(null, 54, color(guiCfg.getString("my-auctions-title")));
                inv.setItem(guiCfg.getInt("buttons.back.slot"), createSimpleItem("buttons.back"));
                p.getScheduler().execute(this, () -> p.openInventory(inv), null, 0L);
            } else if (event.getRawSlot() < 45 && event.getCurrentItem() != null) {
                openConfirmGUI(p, event.getCurrentItem());
            }
        } else if (title.contains("Confirm Purchase")) {
            event.setCancelled(true);
            if (event.getRawSlot() == guiCfg.getInt("confirm-gui.cancel.slot")) {
                playSound(p, "click");
                openMainGUI(p, "recently_listed", "all");
            } else if (event.getRawSlot() == guiCfg.getInt("confirm-gui.accept.slot")) {
                if (p.getInventory().firstEmpty() == -1) {
                    playSound(p, "error");
                    sendMessage(p, "inventory-full", null, null);
                    return;
                }
                playSound(p, "success");
                ItemStack bought = event.getInventory().getItem(13);
                if (bought != null) p.getInventory().addItem(bought);
                p.closeInventory();
                sendMessage(p, "buy-success", "Item", "1000");
            }
        } else if (title.contains("Your Auctions")) {
            event.setCancelled(true);
            if (event.getRawSlot() == guiCfg.getInt("buttons.back.slot")) {
                playSound(p, "click");
                openMainGUI(p, "recently_listed", "all");
            }
        }
    }
}
