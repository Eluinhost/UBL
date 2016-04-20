package gg.uhc.ubl

import gg.uhc.ubl.parser.UblParser
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.plugin.Plugin

open class UblHandler(
    val plugin: Plugin,
    val liveParser: UblParser,
    val backupsParser: UblParser,
    val notInitializedMessage: String,
    val period: Long
)
: Listener {
    protected var initialized: Boolean = false
    protected var entries = listOf<UblEntry>()

    protected open fun loadLive() = try {
        entries = liveParser.fetchAllRecords()
        initialized = true
        saveBackup()
    } catch (ex: Throwable) {
        plugin.logger.severe("Failed to load from live UBL: ${ex.message}")
    }

    protected open fun saveBackup() = try {
        backupsParser.saveRecords(entries)
    } catch (ex: Throwable) {
        plugin.logger.severe("Failed to save backup UBL: ${ex.message}")
    }

    open fun loadBackup() = try {
        entries = backupsParser.fetchAllRecords()
        initialized = true
    } catch (ex: Throwable) {
        plugin.logger.severe("Failed to load from backup UBL: ${ex.message}")
    }

    open fun start() = plugin.server.scheduler.scheduleAsyncRepeatingTask(plugin, { loadLive() }, 0, period)

    @EventHandler open fun on(event: AsyncPlayerPreLoginEvent) {
        if (!initialized) {
            event.loginResult = AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST
            event.kickMessage = notInitializedMessage
            return
        }

        // TODO check entries
    }
}