package gg.uhc.ubl

import java.util.Date
import java.util.UUID

data class UblEntry(
        val caseUrl: String,
        val banned: Date,
        val expires: Date,
        val ign: String,
        val lengthOfBan: String,
        val reason: String,
        val uuid: UUID
)