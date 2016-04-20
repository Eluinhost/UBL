package gg.uhc.ubl.parser

import gg.uhc.ubl.UblEntry
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private const val CASE_URL_KEY = "caseUrl"
private const val BANNED_KEY = "banned"
private const val EXPIRES_KEY = "expires"
private const val IGN_KEY = "ign"
private const val LENGTH_OF_BAN_KEY = "lengthofban"
private const val REASON_KEY = "reason"

open class BackupsUblParser(val backupFile: File) : UblParser {
    init {
        if (backupFile.exists().not()) {
            backupFile.createNewFile()
        }
    }

    override fun saveRecords(records: Map<UUID, UblEntry>) {
        val newConfig = YamlConfiguration()

        records.forEach {
            val section = newConfig.createSection(it.key.toString())

            section.set(CASE_URL_KEY, it.value.caseUrl)
            section.set(BANNED_KEY, it.value.banned)
            section.set(EXPIRES_KEY, DATE_FORMAT.format(it.value.expires))
            section.set(IGN_KEY, it.value.ign)
            section.set(LENGTH_OF_BAN_KEY, it.value.lengthOfBan)
            section.set(REASON_KEY, it.value.reason)
        }

        newConfig.save(backupFile)
    }

    override fun fetchAllRecords(): Map<UUID, UblEntry> {
        val config = YamlConfiguration.loadConfiguration(backupFile)

        return config
            .getKeys(false)
            .map { Pair(UUID.fromString(it), config.getConfigurationSection(it)) }
            .associate {Pair(
                it.first,
                UblEntry(
                    caseUrl = it.second.getString(CASE_URL_KEY),
                    banned = it.second.getString(BANNED_KEY),
                    expires = DATE_FORMAT.parse(it.second.getString(EXPIRES_KEY)),
                    ign = it.second.getString(IGN_KEY),
                    lengthOfBan = it.second.getString(LENGTH_OF_BAN_KEY),
                    reason = it.second.getString(REASON_KEY)
                )
            )}
    }

    companion object DATE_FORMAT : SimpleDateFormat("yyyy-MM-dd")
}
