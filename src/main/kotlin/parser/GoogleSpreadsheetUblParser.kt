package gg.uhc.ubl.parser

import com.google.common.base.Joiner
import com.google.common.io.Resources
import gg.uhc.ubl.UblEntry
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import org.json.simple.parser.ParseException
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.logging.Logger

open class GoogleSpreadsheetUblParser(documentId: String, worksheetId: String, val dateFormat: SimpleDateFormat, val fieldNames: GoogleSpreadSheetColumnNames, val headerRows: Int, val logger: Logger) : UblParser {
    val URL_FORMAT = "https://spreadsheets.google.com/feeds/list/%s/%s/public/values?alt=json"
    val fetchUrl = URL(String.format(URL_FORMAT, documentId, worksheetId))

    val nonAlphaRegex = Regex("[^a-zA-Z0-9]")

    override fun fetchAllRecords() : List<UblEntry> {
        // Read in the contents from the URL
        val raw = Resources.toString(fetchUrl, Charsets.UTF_8)

        return processRawJSON(raw)
    }

    open fun processRawJSON(rawJSON: String) : List<UblEntry> {
        val outerObject = parseOuterObject(rawJSON)
        val entryList = grabEntriesList(outerObject)

        return convertEntryList(entryList)
    }

    open fun convertEntryList(entryList: JSONArray) : List<UblEntry> {
        return entryList
                .filterNotNull()
                .drop(headerRows)
                .mapIndexedNotNull({ index, entry ->
                    try {
                        parseEntry(entry as JSONObject)
                    } catch (ex: InvalidDocumentFormatException) {
                        logger.warning("Skipping row number ${index + 1}. Error message: ${ex.message}}")
                        null
                    }
                })
    }

    open fun parseOuterObject(rawJsonString: String) : JSONObject {
        val rawJson: Any = try {
            JSONParser().parse(rawJsonString)
        } catch (ex: ParseException) {
            throw InvalidDocumentFormatException("Unable to parse JSON, not in valid format")
        }

        if (rawJson !is JSONObject) throw InvalidDocumentFormatException("Expected JSON to be an json object, found ${rawJson.javaClass.name} instead")

        return rawJson
    }

    open fun grabEntriesList(outerObject: JSONObject) : JSONArray {
        val feedObject = outerObject["feed"]

        when (feedObject) {
            null -> throw InvalidDocumentFormatException("Expected outer key `feed` but it was not found")
            !is JSONObject -> throw InvalidDocumentFormatException("Expected JSON to be an json object, found ${feedObject.javaClass.name} instead")
        }

        val entryList = (feedObject as JSONObject)["entry"]

        when (entryList) {
            null -> throw InvalidDocumentFormatException("Expected key `feed.entry` but it was not found")
            !is JSONArray -> throw InvalidDocumentFormatException("Expected JSON to be an json array, found ${feedObject.javaClass.name} instead")
        }

        return entryList as JSONArray
    }

    open fun parseEntry(entryObject: JSONObject) : UblEntry {
        if (entryObject !is JSONObject) throw InvalidDocumentFormatException("Expected entry item to be an json object, found ${entryObject.javaClass.name} instead")

        // Parse each item individually
        val caseUrl = parseEntryString(entryObject, fieldNames.caseUrl)
        val dateBanned = parseEntryString(entryObject, fieldNames.dateBanned)
        val ign = parseEntryString(entryObject, fieldNames.ign)
        val lengthOfBan = parseEntryString(entryObject, fieldNames.lengthOfBan)
        val reason = parseEntryString(entryObject, fieldNames.reason)

        var uuidString = parseEntryString(entryObject, fieldNames.uuid)
        // Parse the UUID from the string version
        val uuid = try {
            if (uuidString.contains('-').not()) {
                if (uuidString.length != 32) throw IllegalArgumentException()

                val seq = uuidString.asSequence().toMutableList()

                seq.add(20, '-')
                seq.add(16, '-')
                seq.add(12, '-')
                seq.add(8, '-')

                uuidString = Joiner.on("").join(seq)
            }


            UUID.fromString(uuidString)
        } catch (ex: IllegalArgumentException) {
            throw InvalidDocumentFormatException("Invalid uuid - $uuidString")
        }

        val expiryDateString = parseEntryString(entryObject, fieldNames.expiryDate)
        // Parse date from the string version
        val dateExpires = try {
            dateFormat.parse(
                expiryDateString
                        .replace("Janurary", "January", true) // Some people just can't spell
                        .replace("Feburary", "February", true) // Some people just can't spell
                        .replace(nonAlphaRegex, "") // Punctuation/spacing differs sometimes, strip everything but alphanumerics
            )
        } catch (ex: java.text.ParseException) {
            logger.info("Unable to parse the date $expiryDateString for $uuid, using 'forever' instead")
            null
        }

        return UblEntry(
                caseUrl = caseUrl,
                ign = ign,
                reason = reason,
                lengthOfBan = lengthOfBan,
                uuid = uuid,
                banned = dateBanned,
                expires = dateExpires
        )
    }

    open fun parseEntryString(entryItem: JSONObject, key: String) : String {
        val item = entryItem["gsx${'$'}$key"]

        when (item) {
            null -> throw InvalidDocumentFormatException("Expected entry key `gsx${'$'}$key` but it was not found")
            !is JSONObject -> throw InvalidDocumentFormatException("Expected entry key ${'$'}$key to be a Object, found ${item.javaClass.name} instead")
            else -> {
                val value = item["${'$'}t"]

                when (value) {
                    null -> throw InvalidDocumentFormatException("Expected entry key `${'$'}$key.${'$'}t` but it was not found")
                    !is String -> throw InvalidDocumentFormatException("Expected entry key ${'$'}$key.${'$'}t to be a String, found ${value.javaClass.name} instead")
                    else -> return value.trim()
                }
            }
        }
    }

    class InvalidDocumentFormatException(message: String) : Exception(message)

    data class GoogleSpreadSheetColumnNames(
        val caseUrl: String,
        val dateBanned: String,
        val expiryDate: String,
        val ign: String,
        val lengthOfBan: String,
        val reason: String,
        val uuid: String
    )
}