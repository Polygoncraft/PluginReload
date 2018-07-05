package me.killje.pluginreload;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 *
 * @author Patrick Beuks (killje) <patrick.beuks@gmail.com>
 */
public class PluginReload extends JavaPlugin {

    @Override
    public void onEnable() {
        getCommand("pluginenable").setExecutor(new CommandExecutor() {
            @Override
            public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
                if (args.length < 1) {
                    return false;
                }
                Plugin plugin = getPlugin(args[0]);
                if (plugin == null) {
                    sender.sendMessage("Could not find plugin with name: " + args[0]);
                    return true;
                }
                if (plugin.isEnabled()) {
                    sender.sendMessage("Plugin is already enabled.");
                    return true;
                }
                Bukkit.getPluginManager().enablePlugin(plugin);
                return true;
            }
        });
        getCommand("plugindisable").setExecutor(new CommandExecutor() {
            @Override
            public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
                if (args.length < 1) {
                    return false;
                }
                Plugin plugin = getPlugin(args[0]);
                if (plugin == null) {
                    sender.sendMessage("Could not find plugin with name: " + args[0]);
                    return true;
                }
                if (!plugin.isEnabled()) {
                    sender.sendMessage("Plugin is already disabled.");
                    return true;
                }
                Bukkit.getPluginManager().disablePlugin(plugin);
                return true;
            }
        });
        getCommand("pluginreload").setExecutor(new CommandExecutor() {
            @Override
            public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
                if (args.length < 1) {
                    return false;
                }
                Plugin plugin = getPlugin(args[0]);
                if (plugin == null) {
                    sender.sendMessage("Could not find plugin with name: " + args[0]);
                    return true;
                }
                if (!plugin.isEnabled()) {
                    sender.sendMessage("Plugin is disabled. Enable it with pluginenable instead");
                    return true;
                }
                Bukkit.getPluginManager().enablePlugin(plugin);
                Bukkit.getPluginManager().disablePlugin(plugin);
                return true;
            }
        });
    }
    
    
    private Plugin getPlugin(String pluginName) {
        
        Plugin xpStorage = Bukkit.getPluginManager().getPlugin(pluginName);
        
        return xpStorage;
        
    }
    
}
