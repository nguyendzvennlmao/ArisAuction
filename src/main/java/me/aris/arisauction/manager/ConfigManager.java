package me.aris.arisauction.manager;

import me.aris.arisauction.ArisAuction;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigManager {
    private final ArisAuction plugin;
    private FileConfiguration config, messages;
    private Map<String, FileConfiguration> guiConfigs = new HashMap<>();

    public ConfigManager(ArisAuction plugin) { this.plugin = plugin; }

    public void loadAllConfigs() {
        reloadConfig(); reloadMessages();
        loadGUI("auction.yml"); loadGUI("youritems.yml");
        loadGUI("transactions.yml"); loadGUI("confirmsell.yml");
    }
    public void reloadAll() { loadAllConfigs(); }
    public void reloadConfig() { plugin.reloadConfig(); config = plugin.getConfig(); }
    public void reloadMessages() { messages = loadFile("messages.yml"); }
    private void loadGUI(String file) { guiConfigs.put(file.replace(".yml",""), loadFile("gui/"+file)); }
    private FileConfiguration loadFile(String path) {
        File f = new File(plugin.getDataFolder(), path);
        if (!f.exists()) { f.getParentFile().mkdirs(); plugin.saveResource(path, false); }
        return YamlConfiguration.loadConfiguration(f);
    }
    public FileConfiguration getConfig() { if (config==null) reloadConfig(); return config; }
    public FileConfiguration getMessages() { if (messages==null) reloadMessages(); return messages; }
    public FileConfiguration getGUI(String name) { return guiConfigs.get(name); }
    public String getMessage(String key) {
        String msg = getMessages().getString(key);
        return msg == null ? "&cMissing: "+key : colorize(msg);
    }
    public List<String> getMessageList(String key) {
        List<String> list = getMessages().getStringList(key);
        if (list != null) for (int i=0; i<list.size(); i++) list.set(i, colorize(list.get(i)));
        return list;
    }
    public String colorize(String text) {
        if (text == null) return "";
        Matcher m = Pattern.compile("&#([A-Fa-f0-9]{6})").matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) m.appendReplacement(sb, ChatColor.of("#"+m.group(1)).toString());
        m.appendTail(sb);
        return ChatColor.translateAlternateColorCodes('&', sb.toString());
    }
    public List<String> colorizeList(List<String> list) {
        if (list == null) return null;
        List<String> out = new ArrayList<>();
        for (String s : list) out.add(colorize(s));
        return out;
    }
            }
