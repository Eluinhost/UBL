package gg.uhc.ubl

import org.bukkit.plugin.java.JavaPlugin

class Entry() : JavaPlugin() {
    override fun onEnable() {
        config.options().copyDefaults(true)
        saveConfig()
    }
}