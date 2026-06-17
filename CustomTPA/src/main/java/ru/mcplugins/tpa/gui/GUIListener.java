package ru.mcplugins.tpa.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;
import ru.mcplugins.tpa.TPAPlugin;
import ru.mcplugins.tpa.manager.ConfigManager;
import ru.mcplugins.tpa.manager.RequestManager;

import java.util.UUID;

public class GUIListener implements Listener {

    private final TPAPlugin plugin;
    private final RequestManager requestManager;

    public GUIListener(TPAPlugin plugin, RequestManager requestManager) {
        this.plugin = plugin;
        this.requestManager = requestManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;

        ConfigManager config = plugin.getConfigManager();
        if (e.getView().title().equals(config.getMessage(player, "gui-title"))) {
            e.setCancelled(true);
            handleRequestGUI(e, player);
        }
    }

    private void handleRequestGUI(InventoryClickEvent e, Player player) {
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        ConfigManager config = plugin.getConfigManager();

        if (clicked.getType() == Material.PLAYER_HEAD && e.getSlot() >= 9 && e.getSlot() <= 17) {
            SkullMeta meta = (SkullMeta) clicked.getItemMeta();
            if (meta.hasOwner()) {
                UUID senderUUID = meta.getOwningPlayer().getUniqueId();
                RequestManager.TPARequest req = requestManager.getRequest(senderUUID, player.getUniqueId());
                if (req != null) {
                    GUIHandler.setSelected(player.getUniqueId(), senderUUID);
                    GUIHandler.openGUI(player, requestManager, config);
                }
            }
            return;
        }

        if (clicked.getType() == Material.LIME_CONCRETE) {
            UUID selected = GUIHandler.getSelected(player.getUniqueId());
            if (selected == null) {
                player.sendMessage(config.getMessage(player, "no-selection"));
                return;
            }
            RequestManager.TPARequest req = requestManager.getRequest(selected, player.getUniqueId());
            if (req == null) {
                player.sendMessage(config.getMessage(player, "request-expired"));
                GUIHandler.clearSelected(player.getUniqueId());
                GUIHandler.openGUI(player, requestManager, config);
                return;
            }
            Player sender = Bukkit.getPlayer(req.getSender());
            if (sender == null || !sender.isOnline()) {
                player.sendMessage(config.getMessage(player, "player-offline"));
                requestManager.removeRequest(req.getSender(), req.getTarget());
                GUIHandler.clearSelected(player.getUniqueId());
                GUIHandler.openGUI(player, requestManager, config);
                return;
            }

            requestManager.removeRequest(req.getSender(), req.getTarget());
            int delay = config.getInt("delay-seconds", 3);
            boolean isHere = req.isHere();

            final Player teleporting = isHere ? player : sender;
            final Player other = isHere ? sender : player;

            teleporting.sendMessage(
                config.format(teleporting, other, "teleport-accepted")
                    .replaceText(builder -> builder.matchLiteral("%seconds%").replacement(String.valueOf(delay)))
            );

            if (delay == 0) {
                if (isHere) player.teleport(sender.getLocation());
                else sender.teleport(player.getLocation());

                player.sendMessage(config.format(player, sender, "request-accepted"));
                sender.sendMessage(config.format(sender, player, "request-accepted"));
            } else {
                new BukkitRunnable() {
                    int countdown = delay;
                    @Override
                    public void run() {
                        if (!teleporting.isOnline() || !other.isOnline()) {
                            if (teleporting.isOnline()) teleporting.sendMessage(config.getMessage(teleporting, "player-offline"));
                            if (other.isOnline()) other.sendMessage(config.getMessage(other, "player-offline"));
                            cancel();
                            return;
                        }
                        if (countdown <= 0) {
                            if (isHere) player.teleport(sender.getLocation());
                            else sender.teleport(player.getLocation());

                            teleporting.sendMessage(config.format(teleporting, other, "request-accepted"));
                            other.sendMessage(config.format(other, teleporting, "request-accepted"));
                            cancel();
                            return;
                        }
                        String msgText = config.getRawMessage(teleporting, "teleport-delay")
                                .replace("%seconds%", String.valueOf(countdown));
                        Component actionBarMsg = config.formatRaw(null, null, msgText);
                        teleporting.sendActionBar(actionBarMsg);
                        countdown--;
                    }
                }.runTaskTimer(plugin, 0L, 20L);
            }

            GUIHandler.clearSelected(player.getUniqueId());
            player.closeInventory();
            return;
        }

        if (clicked.getType() == Material.RED_CONCRETE) {
            UUID selected = GUIHandler.getSelected(player.getUniqueId());
            if (selected == null) {
                player.sendMessage(config.getMessage(player, "no-selection"));
                return;
            }
            RequestManager.TPARequest req = requestManager.getRequest(selected, player.getUniqueId());
            if (req == null) {
                player.sendMessage(config.getMessage(player, "request-expired"));
                GUIHandler.clearSelected(player.getUniqueId());
                GUIHandler.openGUI(player, requestManager, config);
                return;
            }
            Player sender = Bukkit.getPlayer(req.getSender());
            if (sender != null && sender.isOnline()) {
                sender.sendMessage(config.format(sender, player, "request-denied"));
            }
            requestManager.removeRequest(req.getSender(), req.getTarget());

            player.sendMessage(config.format(player, sender, "request-denied"));

            GUIHandler.clearSelected(player.getUniqueId());
            player.closeInventory();
            return;
        }

        if (clicked.getType() == Material.CLOCK) {
            GUIHandler.openGUI(player, requestManager, config);
        }
    }
}