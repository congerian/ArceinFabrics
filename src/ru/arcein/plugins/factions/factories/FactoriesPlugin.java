package ru.arcein.plugins.factions.factories;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;


public class FactoriesPlugin extends JavaPlugin {

    public void onEnable() {
        this.saveDefaultConfig();
        FactoriesManager manager = new FactoriesManager(this);
        Bukkit.getLogger().info("[ARCEIN] Factories has been enabled!");
    }

    public void onDisable(){
        Bukkit.getLogger().info("[ARCEIN] Factories has been disabled.");
    }
}
