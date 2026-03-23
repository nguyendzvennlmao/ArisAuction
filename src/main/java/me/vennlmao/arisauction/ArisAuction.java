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
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArisAuction extends JavaPlugin implements Listener, CommandExecutor {

    private static Economy econ = null;
    private FileConfiguration guiCfg, msgCfg;
    private final Pattern HEX = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private final Map<UUID, String> playerFilter = new HashMap<>();

    @Override
    public void onEnable() {
        setupEconomy();
        saveDefaultConfig();
        loadFiles();
        getCommand("ah").setExecutor(this);
        getServer().getPluginManager().registerEvents(this, this);
    }

    private void loadFiles() {
        File fGui = new File(getDataFolder(), "gui.yml");
        if (!fGui.exists()) saveResource("gui.yml", false);
        guiCfg = YamlConfiguration.loadConfiguration(fGui);
        File fMsg = new File(getDataFolder(), "message.yml");
        if (!fMsg.exists()) saveResource("message.yml", false);
        msgCfg = YamlConfiguration.loadConfiguration(fMsg);
    }

    private void setupEconomy() {
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp != null) econ = rsp.getProvider();
    }

    public String color(String msg) {
        if (msg == null) return "";
        Matcher m = HEX.matcher(msg);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(sb, ChatColor.of("#" + m.group(1)).toString());
        }
        return ChatColor.translateAlternateColorCodes('&', m.appendTail(sb).toString());
    }

    private void sendMsg(Player p, String key, String item, String price) {
        if (!msgCfg.contains(key)) return;
        String raw = msgCfg.getString(key + ".text")
                .replace("%prefix%", msgCfg.getString("prefix", ""))
                .replace("%item%", item != null ? item : "")
                .replace("%price%", price != null ? price : "");
        String formatted = color(raw);
        if (msgCfg.getBoolean(key + ".chat")) p.sendMessage(formatted);
        if (msgCfg.getBoolean(key + ".actionbar")) p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(formatted));
    }

    private void play(Player p, String t) {
        String s = getConfig().getString("sounds." + t);
        if (s != null) p.playSound(p.getLocation(), Sound.valueOf(s), 1f, 1f);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender instanceof Player p) openMain(p, 1);
        return true;
    }

    public void openMain(Player p, int page) {
        play(p, "open-menu");
        String title = color(guiCfg.getString("auction.title").replace("%current_page%", String.valueOf(page)));
        Inventory inv = Bukkit.createInventory(null, guiCfg.getInt("auction.size"), title);
        setBtn(inv, "auction.previous");
        setBtn(inv, "auction.refresh");
        setBtn(inv, "auction.next");
        setBtn(inv, "auction.my-items");
        setBtn(inv, "auction.sort");
        setFilterBtn(inv, p);
        p.getScheduler().execute(this, () -> p.openInventory(inv), null, 0L);
    }

    private void setFilterBtn(Inventory inv, Player p) {
        String path = "auction.filter";
        String current = playerFilter.getOrDefault(p.getUniqueId(), "all");
        ItemStack i = new ItemStack(Material.valueOf(guiCfg.getString(path + ".material")));
        ItemMeta m = i.getItemMeta();
        m.setDisplayName(color(guiCfg.getString(path + ".name")));
        List<String> lore = new ArrayList<>();
        List<String> keys = guiCfg.getStringList(path + ".keys");
        List<String> opts = guiCfg.getStringList(path + ".options");
        for (int j = 0; j < keys.size(); j++) {
            String pre = keys.get(j).equals(current) ? guiCfg.getString(path + ".active_prefix") : guiCfg.getString(path + ".inactive_prefix");
            lore.add(color(pre + opts.get(j)));
        }
        m.setLore(lore);
        i.setItemMeta(m);
        inv.setItem(guiCfg.getInt(path + ".slot"), i);
    }

    private void setBtn(Inventory inv, String path) {
        ItemStack i = new ItemStack(Material.valueOf(guiCfg.getString(path + ".material", "STONE")));
        ItemMeta m = i.getItemMeta();
        if (m != null) {
            m.setDisplayName(color(guiCfg.getString(path + ".name")));
            i.setItemMeta(m);
        }
        inv.setItem(guiCfg.getInt(path + ".slot"), i);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getClickedInventory() == null) return;
        Player p = (Player) e.getWhoClicked();
        String t = ChatColor.stripColor(e.getView().getTitle());
        if (t.contains("ᴀᴜᴄᴛɪᴏɴ (Page")) {
            e.setCancelled(true);
            int slot = e.getRawSlot();
            if (slot == guiCfg.getInt("auction.my-items.slot")) openGui(p, "youritem");
            else if (slot == guiCfg.getInt("auction.filter.slot")) cycleFilter(p);
            else if (slot < 45 && e.getCurrentItem() != null) openConfirm(p, e.getCurrentItem(), "confirmbuy");
        } else if (t.contains("Your Items")) {
            e.setCancelled(true);
            if (guiCfg.getIntegerList("youritem.slots").contains(e.getRawSlot()) && e.getCurrentItem() != null) {
                if (p.getInventory().firstEmpty() == -1) { sendMsg(p, "inventory-full", null, null); return; }
                ItemStack item = e.getCurrentItem().clone();
                e.getClickedInventory().setItem(e.getRawSlot(), null);
                p.getInventory().addItem(item);
                play(p, "success");
            }
        }
    }

    private void cycleFilter(Player p) {
        List<String> keys = guiCfg.getStringList("auction.filter.keys");
        String current = playerFilter.getOrDefault(p.getUniqueId(), "all");
        int next = (keys.indexOf(current) + 1) % keys.size();
        playerFilter.put(p.getUniqueId(), keys.get(next));
        play(p, "click");
        openMain(p, 1);
    }

    public void openGui(Player p, String type) {
        play(p, "open-menu");
        Inventory inv = Bukkit.createInventory(null, guiCfg.getInt(type + ".size"), color(guiCfg.getString(type + ".title")));
        if (guiCfg.getConfigurationSection(type) != null) {
            for (String key : guiCfg.getConfigurationSection(type).getKeys(false)) {
                if (guiCfg.contains(type + "." + key + ".slot")) setBtn(inv, type + "." + key);
            }
        }
        p.getScheduler().execute(this, () -> p.openInventory(inv), null, 0L);
    }

    public void openConfirm(Player p, ItemStack item, String type) {
        Inventory inv = Bukkit.createInventory(null, guiCfg.getInt(type + ".size"), color(guiCfg.getString(type + ".title")));
        setBtn(inv, type + ".confirm");
        setBtn(inv, type + ".cancel");
        inv.setItem(guiCfg.getInt(type + ".item-slot"), item);
        p.getScheduler().execute(this, () -> p.openInventory(inv), null, 0L);
    }
  }
