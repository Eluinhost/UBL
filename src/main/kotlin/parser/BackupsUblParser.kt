package gg.uhc.ubl.parser

import gg.uhc.ubl.UblEntry
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private const val CASE_URL_KEY = "caseUrl"
private const val BANNED_KEY = "banned"
private const val EXPIRES_KEY = "expirs"
private const val IGN_KEY = "ign"
private const val LENGTH_OF_BAN_KEY = "lengthofban"
private const val REASON_KEY = "reason"
private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd")

open class BackupsUblParser(val backupFile: File) : UblParser {
    init {
        if (backupFile.exists().not()) {
            backupFile.createNewFile()
        }
    }

    override fun saveRecords(records: List<UblEntry>) {
        val newConfig = YamlConfiguration()

        records.forEach {
            val section = newConfig.createSection(it.uuid.toString())

            section.set(CASE_URL_KEY, it.caseUrl)
            section.set(BANNED_KEY, it.banned)
            section.set(EXPIRES_KEY, DATE_FORMAT.format(it.expires))
            section.set(IGN_KEY, it.ign)
            section.set(LENGTH_OF_BAN_KEY, it.lengthOfBan)
            section.set(REASON_KEY, it.reason)
        }

        newConfig.save(backupFile)
    }

    override fun fetchAllRecords(): List<UblEntry> {
        val config = YamlConfiguration.loadConfiguration(backupFile)

        return config
                .getKeys(false)
                .map { Pair(UUID.fromString(it), config.getConfigurationSection(it)) }
                .map {
                    UblEntry(
                        caseUrl = it.second.getString(CASE_URL_KEY),
                        banned = it.second.getString(BANNED_KEY),
                        expires = DATE_FORMAT.parse(it.second.getString(EXPIRES_KEY)),
                        ign = it.second.getString(IGN_KEY),
                        lengthOfBan = it.second.getString(LENGTH_OF_BAN_KEY),
                        reason = it.second.getString(REASON_KEY),
                        uuid = it.first
                    )
                }
    }
}
