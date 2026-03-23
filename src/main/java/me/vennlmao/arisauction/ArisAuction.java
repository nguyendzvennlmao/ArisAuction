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
import org.bukkit.command.TabCompleter;
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
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArisAuction extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {

    private static Economy econ = null;
    private FileConfiguration guiCfg, msgCfg;
    private Connection connection;
    private final Pattern HEX = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private final Map<UUID, Double> pendingPrice = new HashMap<>();
    private final Map<UUID, String> playerFilter = new HashMap<>();

    @Override
    public void onEnable() {
        setupEconomy();
        saveDefaultConfig();
        loadFiles();
        setupDatabase();
        getCommand("ah").setExecutor(this);
        getCommand("ah").setTabCompleter(this);
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

    private void setupDatabase() {
        String host = getConfig().getString("mysql.host");
        int port = getConfig().getInt("mysql.port");
        String db = getConfig().getString("mysql.database");
        String user = getConfig().getString("mysql.username");
        String pass = getConfig().getString("mysql.password");
        try {
            connection = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + db + "?useSSL=false", user, pass);
            Statement s = connection.createStatement();
            s.execute("CREATE TABLE IF NOT EXISTS aris_auction (id INT AUTO_INCREMENT PRIMARY KEY, seller VARCHAR(36), item LONGTEXT, price DOUBLE, category VARCHAR(20), time LONG)");
            s.execute("CREATE TABLE IF NOT EXISTS aris_trans (id INT AUTO_INCREMENT PRIMARY KEY, player VARCHAR(36), msg TEXT, time LONG)");
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public String color(String msg) {
        if (msg == null) return "";
        Matcher m = HEX.matcher(msg);
        StringBuilder sb = new StringBuilder();
        while (m.find()) m.appendReplacement(sb, ChatColor.of("#" + m.group(1)).toString());
        return ChatColor.translateAlternateColorCodes('&', m.appendTail(sb).toString());
    }

    private void sendMsg(Player p, String key, String item, String price) {
        if (!msgCfg.contains(key)) return;
        String raw = msgCfg.getString(key + ".text").replace("%prefix%", msgCfg.getString("prefix", "")).replace("%item%", item != null ? item : "").replace("%price%", price != null ? price : "");
        String f = color(raw);
        if (msgCfg.getBoolean(key + ".chat")) p.sendMessage(f);
        if (msgCfg.getBoolean(key + ".actionbar")) p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(f));
    }

    private void play(Player p, String t) {
        String s = getConfig().getString("sounds." + t);
        if (s != null) p.playSound(p.getLocation(), Sound.valueOf(s), 1f, 1f);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) return true;
        if (args.length >= 2 && args[0].equalsIgnoreCase("sell")) {
            try {
                double price = Double.parseDouble(args[1]);
                ItemStack hand = p.getInventory().getItemInMainHand();
                if (hand.getType() == Material.AIR) { sendMsg(p, "no-item", null, null); return true; }
                pendingPrice.put(p.getUniqueId(), price);
                openConfirmSell(p, hand, price);
            } catch (NumberFormatException e) { sendMsg(p, "invalid-price", null, null); }
            return true;
        }
        openMain(p, 1);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String a, String[] args) {
        if (args.length == 1) return Collections.singletonList("sell");
        if (args.length == 2 && args[0].equalsIgnoreCase("sell")) return Collections.singletonList("<price>");
        return Collections.emptyList();
    }

    public void openMain(Player p, int page) {
        play(p, "open-menu");
        Inventory inv = Bukkit.createInventory(null, guiCfg.getInt("auction.size"), color(guiCfg.getString("auction.title").replace("%current_page%", String.valueOf(page))));
        setBtn(inv, "auction.previous");
        setBtn(inv, "auction.refresh");
        setBtn(inv, "auction.next");
        setBtn(inv, "auction.my-items");
        setFilterBtn(inv, p);
        p.getScheduler().execute(this, () -> p.openInventory(inv), null, 0L);
    }

    public void openConfirmSell(Player p, ItemStack item, double price) {
        String type = "confirmsell";
        Inventory inv = Bukkit.createInventory(null, guiCfg.getInt(type + ".size"), color(guiCfg.getString(type + ".title")));
        setBtn(inv, type + ".confirm");
        setBtn(inv, type + ".cancel");
        ItemStack d = item.clone();
        ItemMeta m = d.getItemMeta();
        List<String> lore = new ArrayList<>();
        for (String s : getConfig().getStringList("auction.item-lore")) lore.add(color(s.replace("%seller%", p.getName()).replace("%price%", String.valueOf(price)).replace("%time%", "24h")));
        m.setLore(lore);
        d.setItemMeta(m);
        inv.setItem(guiCfg.getInt(type + ".item-slot"), d);
        p.getScheduler().execute(this, () -> p.openInventory(inv), null, 0L);
    }

    private void setFilterBtn(Inventory inv, Player p) {
        String path = "auction.filter";
        String current = playerFilter.getOrDefault(p.getUniqueId(), "ALL");
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
        if (m != null) { m.setDisplayName(color(guiCfg.getString(path + ".name"))); i.setItemMeta(m); }
        inv.setItem(guiCfg.getInt(path + ".slot"), i);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getClickedInventory() == null) return;
        Player p = (Player) e.getWhoClicked();
        String t = ChatColor.stripColor(e.getView().getTitle());
        if (t.contains("ᴀᴜᴄᴛɪᴏɴ (Page")) {
            e.setCancelled(true);
            if (e.getRawSlot() == guiCfg.getInt("auction.filter.slot")) cycleFilter(p);
            if (e.getRawSlot() == guiCfg.getInt("auction.my-items.slot")) openGui(p, "youritem");
        }
        if (t.contains("ᴄᴏɴꜰɪʀᴍ ʟɪꜱᴛɪɴɢ")) {
            e.setCancelled(true);
            if (e.getRawSlot() == guiCfg.getInt("confirmsell.confirm.slot")) {
                ItemStack item = p.getInventory().getItemInMainHand();
                if (item.getType() != Material.AIR) {
                    double price = pendingPrice.getOrDefault(p.getUniqueId(), 0.0);
                    saveToDB(p, item, price);
                    p.getInventory().setItemInMainHand(null);
                    p.closeInventory();
                    sendMsg(p, "sell-success", item.getType().name(), String.valueOf(price));
                    play(p, "success");
                }
            } else if (e.getRawSlot() == guiCfg.getInt("confirmsell.cancel.slot")) p.closeInventory();
        }
        if (t.contains("ʏᴏᴜʀ ɪᴛᴇᴍꜱ")) {
            e.setCancelled(true);
            if (e.getRawSlot() == guiCfg.getInt("youritem.transactions.slot")) openGui(p, "transauction");
        }
        if (t.contains("ᴛʀᴀɴꜱᴀᴄᴛɪᴏɴꜱ")) {
            e.setCancelled(true);
            if (e.getRawSlot() == guiCfg.getInt("transauction.back.slot")) openGui(p, "youritem");
        }
    }

    private void cycleFilter(Player p) {
        List<String> keys = guiCfg.getStringList("auction.filter.keys");
        String current = playerFilter.getOrDefault(p.getUniqueId(), "ALL");
        int nextIdx = (keys.indexOf(current) + 1) % keys.size();
        playerFilter.put(p.getUniqueId(), keys.get(nextIdx));
        play(p, "click");
        openMain(p, 1);
    }

    public void openGui(Player p, String type) {
        play(p, "open-menu");
        Inventory inv = Bukkit.createInventory(null, guiCfg.getInt(type + ".size"), color(guiCfg.getString(type + ".title")));
        if (guiCfg.contains(type + ".back")) setBtn(inv, type + ".back");
        if (guiCfg.contains(type + ".transactions")) setBtn(inv, type + ".transactions");
        if (guiCfg.contains(type + ".info")) setBtn(inv, type + ".info");
        p.getScheduler().execute(this, () -> p.openInventory(inv), null, 0L);
    }

    private void saveToDB(Player p, ItemStack item, double price) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataStream = new BukkitObjectOutputStream(outputStream);
            dataStream.writeObject(item);
            dataStream.close();
            String base64 = Base64.getEncoder().encodeToString(outputStream.toByteArray());
            PreparedStatement ps = connection.prepareStatement("INSERT INTO aris_auction (seller, item, price, category, time) VALUES (?, ?, ?, ?, ?)");
            ps.setString(1, p.getUniqueId().toString());
            ps.setString(2, base64);
            ps.setDouble(3, price);
            ps.setString(4, getCategory(item.getType()));
            ps.setLong(5, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private String getCategory(Material m) {
        String n = m.name();
        if (n.contains("SWORD") || n.contains("BOW") || n.contains("AXE")) return "COMBAT";
        if (n.contains("PICKAXE") || n.contains("SHOVEL") || n.contains("HOE")) return "TOOLS";
        if (m.isEdible()) return "FOOD";
        if (m.isBlock()) return "BLOCKS";
        return "ALL";
    }
    }
