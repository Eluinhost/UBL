package gg.uhc.ubl.parser

import gg.uhc.ubl.UblEntry

interface UblParser {
    open fun fetchAllRecords() : List<UblEntry>
}
