package ru.arcein.plugins.factions.factories;

//import com.massivecraft.factions.entity.MPlayer;
//import com.massivecraft.factions.event.EventFactionsHomeChange;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import ru.arcein.plugins.factions.factories.Factory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FactoriesManager implements Listener {

    public FactoriesPlugin plugin;
    public List <Factory> factories;

    public FileConfiguration config;

    public class TimerTask extends BukkitRunnable{
        public FactoriesManager manager;

        public TimerTask(FactoriesManager manager){
            this.manager = manager;
        }

        @Override
        public void run() {
            for(Factory factory : this.manager.factories){
                factory.startIfTime();
            }
        }
    }

    public FactoriesManager(FactoriesPlugin plugin){
        this.plugin = plugin;
        this.config = plugin.getConfig();
        this.factories = getFactoriesFromConfig();
        this.plugin.getServer().getPluginManager().registerEvents(new FactoryListener(this), plugin);
        this.plugin.getCommand("factoriesreload").setExecutor(new FactoryCommandExecutor(this));
        BukkitTask task = new TimerTask(this).runTaskTimer(this.plugin, 1L, 20L);
    }

    private List<Factory> getFactoriesFromConfig(){
        List <Factory> _factories = new ArrayList<>();

        Set<String> cfg_factories = config.getConfigurationSection("factories").getKeys(false);
        for(String cfg_factory_key : cfg_factories){
            ConfigurationSection cs = config.getConfigurationSection("factories").getConfigurationSection(cfg_factory_key);
            Factory factory = new Factory(
                    this,
                    new Location(
                            this.plugin.getServer().getWorld("world"),
                            cs.getDouble("x"),
                            cs.getDouble("y"),
                            cs.getDouble("z")),
                    cs.getString("start"),
                    cs.getString("end"),
                    cs.getString("name"),
                    cs.getDouble("radius"),
                    cs.getDouble("radius-to-announce"),
                    cs.getDouble("no-home-range"),
                    cs.getLong("seconds-to-win"),
                    cs.getDouble("reward")
            );
            _factories.add(factory);
        }

        return _factories;
    }

    private class FactoryCommandExecutor implements CommandExecutor{

        FactoriesManager manager;

        public FactoryCommandExecutor(FactoriesManager manager){
            this.manager = manager;
        }

        @Override
        public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
            if(commandSender.isOp()){
                for(Factory f : this.manager.factories){
                    f.isDeleted = true;
                }
                this.manager.plugin.reloadConfig();
                this.manager.config = this.manager.plugin.getConfig();
                this.manager.factories = this.manager.getFactoriesFromConfig();
            }
            return true;
        }
    }

    private class FactoryListener implements Listener{

        FactoriesManager manager;

        public FactoryListener(FactoriesManager manager){
            this.manager = manager;
        }

        /*@EventHandler
        public void factionsChangeHomeEvent(EventFactionsHomeChange event) {
            for(Factory f : manager.factories){
                MPlayer pl = event.getMPlayer();
                if(event.getNewHome().asBukkitLocation().distance(f.loc) < f.noHomeRange) {
                    event.setCancelled(true);
                    if(pl != null) pl.getPlayer().sendMessage(ChatColor.RED +
                            "Запрещено устанавливать клановый хоум в радиусе " + f.noHomeRange + " от фабрики " + f.name + "!");
                }
            }
        }*/
    }

}
