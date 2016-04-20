package gg.uhc.ubl.parser

import gg.uhc.ubl.UblEntry
import java.util.*

interface UblParser {
    open fun fetchAllRecords() : Map<UUID, UblEntry>
    open fun saveRecords(records: Map<UUID, UblEntry>)
}
