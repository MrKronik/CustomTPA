package ru.mcplugins.tpa.manager;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigManager {

    private final JavaPlugin plugin;
    private final Map<String, FileConfiguration> languageCache = new HashMap<>();
    private final Map<UUID, String> playerLanguages = new HashMap<>();
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private static final Pattern OTHER_PLACEHOLDER = Pattern.compile("%other_([^%]+)%");
    private static final Map<String, String> LEGACY_COLORS = new LinkedHashMap<>();

    static {
        LEGACY_COLORS.put("&0", "<black>");    LEGACY_COLORS.put("§0", "<black>");
        LEGACY_COLORS.put("&1", "<dark_blue>"); LEGACY_COLORS.put("§1", "<dark_blue>");
        LEGACY_COLORS.put("&2", "<dark_green>");LEGACY_COLORS.put("§2", "<dark_green>");
        LEGACY_COLORS.put("&3", "<dark_aqua>");LEGACY_COLORS.put("§3", "<dark_aqua>");
        LEGACY_COLORS.put("&4", "<dark_red>");  LEGACY_COLORS.put("§4", "<dark_red>");
        LEGACY_COLORS.put("&5", "<dark_purple>");LEGACY_COLORS.put("§5", "<dark_purple>");
        LEGACY_COLORS.put("&6", "<gold>");      LEGACY_COLORS.put("§6", "<gold>");
        LEGACY_COLORS.put("&7", "<gray>");      LEGACY_COLORS.put("§7", "<gray>");
        LEGACY_COLORS.put("&8", "<dark_gray>"); LEGACY_COLORS.put("§8", "<dark_gray>");
        LEGACY_COLORS.put("&9", "<blue>");      LEGACY_COLORS.put("§9", "<blue>");
        LEGACY_COLORS.put("&a", "<green>");     LEGACY_COLORS.put("§a", "<green>");
        LEGACY_COLORS.put("&b", "<aqua>");      LEGACY_COLORS.put("§b", "<aqua>");
        LEGACY_COLORS.put("&c", "<red>");       LEGACY_COLORS.put("§c", "<red>");
        LEGACY_COLORS.put("&d", "<light_purple>");LEGACY_COLORS.put("§d", "<light_purple>");
        LEGACY_COLORS.put("&e", "<yellow>");    LEGACY_COLORS.put("§e", "<yellow>");
        LEGACY_COLORS.put("&f", "<white>");     LEGACY_COLORS.put("§f", "<white>");

        LEGACY_COLORS.put("&l", "<bold>");      LEGACY_COLORS.put("§l", "<bold>");
        LEGACY_COLORS.put("&o", "<italic>");    LEGACY_COLORS.put("§o", "<italic>");
        LEGACY_COLORS.put("&n", "<underlined>");LEGACY_COLORS.put("§n", "<underlined>");
        LEGACY_COLORS.put("&m", "<strikethrough>");LEGACY_COLORS.put("§m", "<strikethrough>");
        LEGACY_COLORS.put("&k", "<obfuscated>");LEGACY_COLORS.put("§k", "<obfuscated>");
        LEGACY_COLORS.put("&r", "<reset>");     LEGACY_COLORS.put("§r", "<reset>");
    }

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadLanguages();
    }

    private void loadLanguages() {
        File langFolder = new File(plugin.getDataFolder(), "lang");
        if (!langFolder.exists()) {
            langFolder.mkdirs();
            plugin.saveResource("lang/messages.yml", false);
            plugin.saveResource("lang/messages_ru.yml", false);
            plugin.saveResource("lang/messages_uk.yml", false);
        }

        File[] files = langFolder.listFiles((dir, name) -> name.startsWith("messages") && name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                String fileName = file.getName();
                String langCode = "en";
                if (fileName.contains("_")) {
                    langCode = fileName.substring(fileName.indexOf('_') + 1, fileName.lastIndexOf('.'));
                }
                languageCache.put(langCode, YamlConfiguration.loadConfiguration(file));
            }
        }

        if (!languageCache.containsKey("en")) {
            plugin.getLogger().warning("Английский язык не найден, использую резервный");
        }
    }

    public void setPlayerLanguage(Player player) {
        String locale = player.getLocale();
        String langCode = locale.split("_")[0].toLowerCase();

        if (languageCache.containsKey(langCode)) {
            playerLanguages.put(player.getUniqueId(), langCode);
        } else {
            playerLanguages.put(player.getUniqueId(), "en");
        }
    }

    public void removePlayerLanguage(Player player) {
        playerLanguages.remove(player.getUniqueId());
    }

    public Component getMessage(Player player, String key) {
        String langCode = "en";
        if (player != null) {
            langCode = playerLanguages.getOrDefault(player.getUniqueId(), "en");
        }
        FileConfiguration messages = languageCache.get(langCode);
        if (messages == null) messages = languageCache.get("en");

        String raw = messages.getString(key, "<red>Missing message: " + key + "</red>");
        if (raw.isEmpty()) return Component.empty();
        return miniMessage.deserialize(convertLegacyToMiniMessage(raw));
    }

    public Component getMessage(String key) {
        return getMessage(null, key);
    }

    public String getRawMessage(Player player, String key) {
        String langCode = "en";
        if (player != null) {
            langCode = playerLanguages.getOrDefault(player.getUniqueId(), "en");
        }
        FileConfiguration messages = languageCache.get(langCode);
        if (messages == null) messages = languageCache.get("en");
        return messages.getString(key, "");
    }

    public String getRawMessage(String key) {
        return getRawMessage(null, key);
    }

    public int getInt(String key, int def) {
        FileConfiguration messages = languageCache.get("en");
        if (messages == null) return def;
        return messages.getInt(key, def);
    }

    public Component format(Player receiver, Player other, String key) {
        String raw = getRawMessage(receiver, key);
        if (raw.isEmpty()) return Component.empty();
        return formatRaw(receiver, other, raw);
    }

    public Component formatRaw(Player receiver, Player other, String raw) {
        if (raw.isEmpty()) return Component.empty();

        if (other != null) {
            raw = raw.replace("%other_name%", other.getName());
        } else {
            raw = raw.replace("%other_name%", "Unknown");
        }

        if (other != null && Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            Matcher matcher = OTHER_PLACEHOLDER.matcher(raw);
            StringBuffer sb = new StringBuffer();
            while (matcher.find()) {
                String placeholder = "%" + matcher.group(1) + "%";
                String value = PlaceholderAPI.setPlaceholders(other, placeholder);
                matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
            }
            matcher.appendTail(sb);
            raw = sb.toString();
        }

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null && receiver != null) {
            raw = PlaceholderAPI.setPlaceholders(receiver, raw);
        }

        return miniMessage.deserialize(convertLegacyToMiniMessage(raw));
    }

    public static String convertLegacyToMiniMessage(String input) {
        if (input == null) return "";

        java.util.regex.Matcher hexMatcher = java.util.regex.Pattern.compile("&#([A-Fa-f0-9]{6})").matcher(input);
        StringBuffer sb = new StringBuffer();
        while (hexMatcher.find()) {
            hexMatcher.appendReplacement(sb, "<#" + hexMatcher.group(1) + ">");
        }
        hexMatcher.appendTail(sb);
        String result = sb.toString();

        for (Map.Entry<String, String> entry : LEGACY_COLORS.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }

        return result;
    }

    public void reload() {
        languageCache.clear();
        loadLanguages();
        playerLanguages.clear();
    }
}