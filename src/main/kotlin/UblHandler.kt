package gg.uhc.ubl

import gg.uhc.ubl.parser.BackupsUblParser
import gg.uhc.ubl.parser.UblParser
import org.bukkit.ChatColor
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
    val excludedUuids: Set<UUID>,
    val kickMessage: String,
    val period: Long
)
: Listener {
    protected var initialized: Boolean = false
    protected var entries = mapOf<UUID, UblEntry>()

    open fun loadLive() = try {
        plugin.logger.info("Starting load of UBL from live data")
        entries = liveParser.fetchAllRecords()
        plugin.logger.info("Live UBL data loaded!")
        initialized = true

        val toKick = plugin.server.onlinePlayers
            .map { Pair(it, entries[it.uniqueId]) }
            .filter { it.second != null }

        plugin.server.scheduler.callSyncMethod(plugin, {
            toKick.forEach {
                it.first.kickPlayer(createKickMessage(it.second!!))
                plugin.server.broadcastMessage("${ChatColor.RED}${it.first.name} was kicked due to being added to the UBL")
            }
        })

        saveBackup()
        true
    } catch (ex: Throwable) {
        plugin.logger.severe("Failed to load from live UBL: ${ex.message}")
        false
    }

    protected open fun saveBackup() = try {
        plugin.logger.info("Saving current UBL to backup file")
        backupsParser.saveRecords(entries)
        plugin.logger.info("Backup completed!")
    } catch (ex: Throwable) {
        plugin.logger.severe("Failed to save backup UBL: ${ex.message}")
    }

    open fun loadBackup() = try {
        plugin.logger.info("Loading UBL from backup data")
        entries = backupsParser.fetchAllRecords()
        plugin.logger.info("Backup UBL loaded!")
        initialized = true
    } catch (ex: Throwable) {
        plugin.logger.severe("Failed to load from backup UBL: ${ex.message}")
    }

    open fun createKickMessage(entry: UblEntry) = this.kickMessage
        .replace("{{caseUrl}}", entry.caseUrl, true)
        .replace("{{banned}}", entry.banned, true)
        .replace("{{lengthOfBan}}", entry.lengthOfBan, true)
        .replace("{{expires}}", BackupsUblParser.DATE_FORMAT.format(entry.expires), true)
        .replace("{{reason}}", entry.reason, true)
        .replace("{{ign}}", entry.ign, true)

    open fun start() = plugin.server.scheduler.scheduleAsyncRepeatingTask(plugin, { loadLive() }, 0, period)

    @EventHandler open fun on(event: AsyncPlayerPreLoginEvent) {
        if (!initialized) {
            event.loginResult = AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST
            event.kickMessage = notInitializedMessage
            return
        }

        if (excludedUuids.contains(event.uniqueId)) return

        val match = entries[event.uniqueId] ?: return

        // UBL entry has already expired
        if (match.expires != null && match.expires.before(Date())) return

        event.loginResult = AsyncPlayerPreLoginEvent.Result.KICK_BANNED
        event.kickMessage = createKickMessage(match)
    }
}
