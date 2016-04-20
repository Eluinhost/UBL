package gg.uhc.ubl

import gg.uhc.ubl.parser.UblParser
import net.md_5.bungee.api.ChatColor
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.plugin.Plugin
import java.util.*

open class UblHandler(
    val plugin: Plugin,
    val liveParser: UblParser,
    val backupsParser: UblParser,
    val notInitializedMessage: String,
    val kickMessage: String,
    val period: Long
)
: Listener {
    protected var initialized: Boolean = false
    protected var entries = mapOf<UUID, UblEntry>()

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

        val match = entries[event.uniqueId] ?: return

        // UBL entry has already expired
        if (match.expires != null && match.expires.before(Date())) return

        event.loginResult = AsyncPlayerPreLoginEvent.Result.KICK_BANNED
        event.kickMessage = ChatColor.translateAlternateColorCodes('&', this.kickMessage.format(
            match.caseUrl,
            match.banned,
            match.lengthOfBan,
            match.expires,
            match.reason,
            match.ign
        ))
    }
}
