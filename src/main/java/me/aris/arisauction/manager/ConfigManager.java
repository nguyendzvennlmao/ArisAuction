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
    private FileConfiguration config;
    private FileConfiguration messages;
    private Map<String, FileConfiguration> guiConfigs;

    public ConfigManager(ArisAuction plugin) {
        this.plugin = plugin;
        this.guiConfigs = new HashMap<>();
    }

    public void loadAllConfigs() {
        reloadConfig();
        reloadMessages();
        loadGUI("auction.yml");
        loadGUI("youritems.yml");
        loadGUI("transactions.yml");
        loadGUI("confirmsell.yml");
    }

    public void reloadAll() {
        loadAllConfigs();
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        config = plugin.getConfig();
    }

    public void reloadMessages() {
        messages = loadFile("messages.yml");
    }

    private void loadGUI(String fileName) {
        guiConfigs.put(fileName.replace(".yml", ""), loadFile("gui/" + fileName));
    }

    private FileConfiguration loadFile(String path) {
        File file = new File(plugin.getDataFolder(), path);
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            plugin.saveResource(path, false);
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    public FileConfiguration getConfig() {
        if (config == null) reloadConfig();
        return config;
    }

    public FileConfiguration getMessages() {
        if (messages == null) reloadMessages();
        return messages;
    }

    public FileConfiguration getGUI(String name) {
        return guiConfigs.get(name);
    }

    public String getMessage(String key) {
        String message = getMessages().getString(key);
        if (message == null) return "&cMessage not found: " + key;
        return colorize(message);
    }

    public List<String> getMessageList(String key) {
        List<String> list = getMessages().getStringList(key);
        if (list == null) return null;
        for (int i = 0; i < list.size(); i++) {
            list.set(i, colorize(list.get(i)));
        }
        return list;
    }

    public String colorize(String text) {
        if (text == null) return "";
        Pattern pattern = Pattern.compile("&#([A-Fa-f0-9]{6})");
        Matcher matcher = pattern.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1);
            matcher.appendReplacement(sb, net.md_5.bungee.api.ChatColor.of("#" + hex).toString());
        }
        matcher.appendTail(sb);
        return ChatColor.translateAlternateColorCodes('&', sb.toString());
    }

    public List<String> colorizeList(List<String> list) {
        if (list == null) return null;
        List<String> colored = new ArrayList<>();
        for (String line : list) {
            colored.add(colorize(line));
        }
        return colored;
    }
    }
