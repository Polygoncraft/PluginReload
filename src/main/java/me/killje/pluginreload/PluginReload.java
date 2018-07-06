package me.killje.pluginreload;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.event.Event;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.plugin.UnknownDependencyException;
import org.bukkit.plugin.java.JavaPlugin;

public class PluginReload extends JavaPlugin {

    @Override
    public void onEnable() {
        getCommand("pluginenable").setExecutor(new CommandExecutor() {
            @Override
            public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
                if (args.length < 1) {
                    return false;
                }
                sender.sendMessage(loadPlugin(args[0]));
                return true;
            }
        });
        getCommand("plugindisable").setExecutor(new CommandExecutor() {
            @Override
            public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
                if (args.length < 1) {
                    return false;
                }

                try {
                    sender.sendMessage(unloadPlugin(args[0]));
                } catch (NullPointerException | NoSuchFieldException | IllegalAccessException ex) {
                    sender.sendMessage(ChatColor.RED + "Failed to unload plugin!");
                }

                return true;
            }
        });
        getCommand("pluginreload").setExecutor(new CommandExecutor() {
            @Override
            public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
                if (args.length < 1) {
                    return false;
                }

                try {
                    sender.sendMessage(unloadPlugin(args[0]));
                    sender.sendMessage(loadPlugin(args[0]));
                } catch (NullPointerException | NoSuchFieldException | IllegalAccessException ex) {
                    sender.sendMessage(ChatColor.RED + "Failed to unload plugin!");
                }
                return true;
            }
        });
    }

    public Plugin getPlugin(String p) {
        for (Plugin pl : getServer().getPluginManager().getPlugins()) {
            if (pl.getDescription().getName().equalsIgnoreCase(p)) {
                return pl;
            }
        }
        return null;
    }

    public String loadPlugin(String pl) {
        Plugin targetPlugin;
        String msg = "";
        File pluginDir = new File("plugins");
        if (!pluginDir.isDirectory()) {
            return (ChatColor.RED + "Plugin directory not found!");
        }
        File pluginFile = new File(pluginDir, pl + ".jar");
        
        if (!pluginFile.isFile()) {
            for (File f : pluginDir.listFiles()) {
                try {
                    if (f.getName().endsWith(".jar")) {
                        PluginDescriptionFile pdf = getPluginLoader().getPluginDescription(f);
                        
                        if (pdf.getName().equalsIgnoreCase(pl)) {
                            pluginFile = f;
                            msg = "(via search) ";
                            break;
                        }
                    }
                } catch (InvalidDescriptionException e) {
                    return (ChatColor.RED + "Couldn't find file and failed to search descriptions!");
                }
            }
        }
        try {
            getServer().getPluginManager().loadPlugin(pluginFile);
            targetPlugin = getPlugin(pl);
            targetPlugin.onLoad();
            getServer().getPluginManager().enablePlugin(targetPlugin);
            return (ChatColor.GREEN + "" + getPlugin(pl) + " loaded " + msg + "and enabled!");
        } catch (UnknownDependencyException e) {
            return (ChatColor.RED + "File exists, but is missing a dependency!");
        } catch (InvalidPluginException e) {
            getLogger().log(Level.SEVERE, "Tried to load invalid Plugin.\n", e);
            return (ChatColor.RED + "File exists, but isn't a loadable plugin file!");
        } catch (InvalidDescriptionException e) {
            return (ChatColor.RED + "Plugin exists, but has an invalid description!");
        }
    }

    public String unloadPlugin(String pl) throws NoSuchFieldException, IllegalAccessException, NullPointerException {

        PluginManager pluginManager = getServer().getPluginManager();
        SimpleCommandMap cmdMap = null;
        List<Plugin> plugins = null;
        Map<String, Plugin> names = null;
        Map<String, Command> commands = null;
        Map<Event, SortedSet<RegisteredListener>> listeners = null;
        boolean reloadlisteners = true;
        if (pluginManager == null) {
            throw new NullPointerException("Could not find pluginManager");
        }
        try {
            Field pluginsField = pluginManager.getClass().getDeclaredField("plugins");
            pluginsField.setAccessible(true);
            plugins = (List<Plugin>) pluginsField.get(pluginManager);

            Field lookupNamesField = pluginManager.getClass().getDeclaredField("lookupNames");
            lookupNamesField.setAccessible(true);
            names = (Map<String, Plugin>) lookupNamesField.get(pluginManager);

            try {
                Field listenersField = pluginManager.getClass().getDeclaredField("listeners");
                listenersField.setAccessible(true);
                listeners = (Map<Event, SortedSet<RegisteredListener>>) listenersField.get(pluginManager);
            } catch (IllegalAccessException | IllegalArgumentException | NoSuchFieldException | SecurityException e) {
                reloadlisteners = false;
            }

            Field commandMapField = pluginManager.getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            cmdMap = (SimpleCommandMap) commandMapField.get(pluginManager);

            Field knownCommandsField = cmdMap.getClass().getDeclaredField("knownCommands");
            knownCommandsField.setAccessible(true);
            commands = (Map<String, Command>) knownCommandsField.get(cmdMap);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw e;
        }

        String tp = "";
        for (Plugin plugin : pluginManager.getPlugins()) {
            if (plugin.getDescription().getName().equalsIgnoreCase(pl)) {
                pluginManager.disablePlugin(plugin);
                tp += plugin.getName() + " ";
                if (plugins != null && plugins.contains(plugin)) {
                    plugins.remove(plugin);
                }

                if (names != null && names.containsKey(pl)) {
                    names.remove(pl);
                }

                if (listeners != null && reloadlisteners) {
                    for (SortedSet<RegisteredListener> set : listeners.values()) {
                        for (Iterator<RegisteredListener> it = set.iterator(); it.hasNext();) {
                            RegisteredListener value = it.next();

                            if (value.getPlugin() == plugin) {
                                it.remove();
                            }
                        }
                    }
                }

                for (Iterator<Map.Entry<String, Command>> it = commands.entrySet().iterator(); it.hasNext();) {
                    Map.Entry<String, Command> entry = it.next();
                    if (entry.getValue() instanceof PluginCommand) {
                        PluginCommand command = (PluginCommand) entry.getValue();
                        if (command.getPlugin() == plugin) {
                            command.unregister(cmdMap);
                            it.remove();
                        }
                    }
                }
            }
        }
        return ChatColor.GREEN + tp + " has been unloaded and disabled!";
    }

}
