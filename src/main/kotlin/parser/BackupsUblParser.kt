package gg.uhc.ubl.parser

import gg.uhc.ubl.UblEntry
import java.io.File

open class BackupsUblParser(val backupFile: File) : UblParser {
    override fun fetchAllRecords(): List<UblEntry> {
        throw UnsupportedOperationException()
    }
}
