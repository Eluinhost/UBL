package gg.uhc.ubl

import java.util.*

data class UblEntry(
        val caseUrl: String,
        val banned: String,
        val expires: Date?,
        val ign: String,
        val lengthOfBan: String,
        val reason: String,
        val uuid: UUID
)