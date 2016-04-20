package gg.uhc.ubl.commands

import gg.uhc.ubl.UblHandler
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.plugin.Plugin

class RefreshUblCommand(val handler: UblHandler, val plugin: Plugin) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        plugin.server.scheduler.runTaskAsynchronously(plugin, {
            sender.sendMessage(if (handler.loadLive()) "UBL refreshed" else "Failed to refresh UBL, check console for more information")
        })
        return true
    }
}
