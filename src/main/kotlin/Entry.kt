package gg.uhc.ubl

import gg.uhc.ubl.commands.RefreshUblCommand
import gg.uhc.ubl.parser.BackupsUblParser
import gg.uhc.ubl.parser.GoogleSpreadsheetUblParser
import net.md_5.bungee.api.ChatColor
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class Entry() : JavaPlugin() {
    override fun onEnable() {
        config.options().copyDefaults(true)
        saveConfig()

        val names = GoogleSpreadsheetUblParser.GoogleSpreadSheetColumnNames(
            caseUrl = config.getString("column names.caseUrl"),
            expiryDate = config.getString("column names.expiryDate"),
            dateBanned = config.getString("column names.dateBanned"),
            ign = config.getString("column names.ign"),
            lengthOfBan = config.getString("column names.lengthOfBan"),
            reason = config.getString("column names.reason"),
            uuid = config.getString("column names.uuid")
        )

        val exlcudedUuids = config.getStringList("excluded uuids")
            .mapNotNull {
                try {
                    UUID.fromString(it)
                } catch (ex: IllegalArgumentException) {
                    null
                }
            }
            .toSet()

        val handler = UblHandler(
            plugin = this,
            liveParser = GoogleSpreadsheetUblParser(
                documentId = config.getString("google spreadsheet id"),
                worksheetId = config.getString("worksheet id"),
                dateFormat = SimpleDateFormat("MMMMMddyyyy"),
                fieldNames = names,
                logger = logger
            ),
            backupsParser = BackupsUblParser(File(dataFolder, "ubl-backup.yml")),
            notInitializedMessage = ChatColor.translateAlternateColorCodes('&', config.getString("waiting first load message")),
            kickMessage = ChatColor.translateAlternateColorCodes('&', config.getString("banned message")),
            period = config.getInt("auto refresh minutes") * 60 * 20L,
            excludedUuids = exlcudedUuids
        )

        handler.loadBackup()
        handler.start()

        server.pluginManager.registerEvents(handler, this)

        getCommand("ublrefresh").executor = RefreshUblCommand(handler, this)
    }
}