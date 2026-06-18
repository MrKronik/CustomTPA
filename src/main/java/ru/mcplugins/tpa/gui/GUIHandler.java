package ru.mcplugins.tpa.gui;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import ru.mcplugins.tpa.manager.ConfigManager;
import ru.mcplugins.tpa.manager.RequestManager;

import java.util.*;
import java.util.stream.Collectors;

public class GUIHandler {

    private static final Map<UUID, UUID> selectedRequest = new HashMap<>();

    public static void openGUI(Player player, RequestManager requestManager, ConfigManager config) {
        List<RequestManager.TPARequest> incoming = requestManager.getIncomingRequests(player.getUniqueId());

        // Заголовок через String (старый метод)
        String title = toLegacy(config.getMessage(player, "gui-title"));
        Inventory inv = Bukkit.createInventory(null, 27, title);

        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.setDisplayName(" ");
        glass.setItemMeta(glassMeta);
        for (int i = 0; i < 9; i++) inv.setItem(i, glass);
        for (int i = 18; i < 27; i++) inv.setItem(i, glass);

        int slot = 9;
        for (RequestManager.TPARequest req : incoming) {
            if (slot > 17) break;
            Player sender = Bukkit.getPlayer(req.getSender());
            if (sender == null) continue;

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta skull = (SkullMeta) head.getItemMeta();
            skull.setOwningPlayer(sender);

            String type = req.isHere() ? "→" : "←";
            String name = toLegacy(config.getMessage(player, "select-player-title")) + " " + sender.getName();
            skull.setDisplayName(name);

            List<String> lore = new ArrayList<>();
            long left = 60 - (System.currentTimeMillis() - req.getTimestamp()) / 1000;

            lore.add(toLegacy(config.getMessage(player, "expires-in")
                    .replaceText(b -> b.matchLiteral("%seconds%").replacement(String.valueOf(Math.max(0, left))))));

            if (req.getSender().equals(selectedRequest.get(player.getUniqueId()))) {
                lore.add("§a§lВЫБРАН");
                skull.setDisplayName("§a✔ " + skull.getDisplayName());
                skull.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
            } else {
                lore.add(toLegacy(config.getMessage(player, "no-selection")));
            }
            skull.setLore(lore);
            head.setItemMeta(skull);
            inv.setItem(slot, head);
            slot++;
        }

        ItemStack accept = new ItemStack(Material.LIME_CONCRETE);
        ItemMeta accMeta = accept.getItemMeta();
        accMeta.setDisplayName(toLegacy(config.getMessage(player, "accept-button-text")));
        accept.setItemMeta(accMeta);
        inv.setItem(20, accept);

        ItemStack deny = new ItemStack(Material.RED_CONCRETE);
        ItemMeta denyMeta = deny.getItemMeta();
        denyMeta.setDisplayName(toLegacy(config.getMessage(player, "deny-button-text")));
        deny.setItemMeta(denyMeta);
        inv.setItem(22, deny);

        ItemStack refresh = new ItemStack(Material.CLOCK);
        ItemMeta refMeta = refresh.getItemMeta();
        refMeta.setDisplayName(toLegacy(config.getMessage(player, "refresh-button")));
        refresh.setItemMeta(refMeta);
        inv.setItem(24, refresh);

        player.openInventory(inv);
    }

    private static String toLegacy(net.kyori.adventure.text.Component component) {
        return LegacyComponentSerializer.legacySection().serialize(component);
    }

    public static void setSelected(UUID player, UUID sender) { selectedRequest.put(player, sender); }
    public static UUID getSelected(UUID player) { return selectedRequest.get(player); }
    public static void clearSelected(UUID player) { selectedRequest.remove(player); }
}