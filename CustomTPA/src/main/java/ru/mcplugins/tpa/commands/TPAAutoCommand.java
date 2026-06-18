package ru.mcplugins.tpa.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.mcplugins.tpa.TPAPlugin;
import ru.mcplugins.tpa.manager.ConfigManager;
import ru.mcplugins.tpa.manager.RequestManager;

public class TPAAutoCommand implements CommandExecutor {
    private final RequestManager requestManager;
    private final ConfigManager config;

    public TPAAutoCommand(RequestManager requestManager, ConfigManager config) {
        this.requestManager = requestManager;
        this.config = config;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Только игроки.");
            return true;
        }
        if (!player.hasPermission("tpa.auto")) {
            player.sendMessage(config.getMessage(player, "no-permission"));
            return true;
        }

        boolean current = requestManager.isAutoAccept(player.getUniqueId());
        requestManager.setAutoAccept(player.getUniqueId(), !current);

        if (!current) {
            player.sendMessage(config.getMessage(player, "auto-accept-on"));
        } else {
            player.sendMessage(config.getMessage(player, "auto-accept-off"));
        }
        return true;
    }
}