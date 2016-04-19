package gg.uhc.ubl

import org.bukkit.configuration.serialization.ConfigurationSerializable
import java.text.SimpleDateFormat
import java.util.*

private val dateFormat = SimpleDateFormat("yyyy-MM-dd")

data class UblEntry(
    val caseUrl: String,
    val banned: String,
    val expires: Date?,
    val ign: String,
    val lengthOfBan: String,
    val reason: String,
    val uuid: UUID
) : ConfigurationSerializable {
    override fun serialize() = mapOf(
        Pair("caseUrl", caseUrl),
        Pair("banned", banned),
        Pair("expires", dateFormat.format(expires)),
        Pair("ign", ign),
        Pair("lengthOfBan", lengthOfBan),
        Pair("reason", reason),
        Pair("uuid", uuid.toString())
    )
}

fun deserialize(map: Map<String, Any>) = UblEntry(
    caseUrl = map["caseUrl"] as String,
    banned = map["banned"] as String,
    expires = dateFormat.parse(map["expires"] as String),
    ign = map["ign"] as String,
    lengthOfBan = map["lengthOfBan"] as String,
    reason = map["reason"] as String,
    uuid = UUID.fromString(map["uuid"] as String)
)