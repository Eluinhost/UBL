package gg.uhc.ubl

import com.google.common.io.Resources
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import org.json.simple.parser.ParseException
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.logging.Logger

open class GoogleSpreadsheetUblFetcher(documentId: String, worksheetId: String, val dateFormat: SimpleDateFormat, val fieldNames: GoogleSpreadSheetColumnNames, val logger: Logger) : UblFetcher {
    val URL_FORMAT = "https://spreadsheets.google.com/feeds/list/%s/%s/public/values?alt=json"
    val fetchUrl = URL(String.format(URL_FORMAT, documentId, worksheetId))

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
                .mapIndexedNotNull({ index, entry ->
                    try {
                        parseEntry(entry as JSONObject)
                    } catch (ex: InvalidDocumentFormatException) {
                        logger.warning("Skipping entry number $index. Error message: ${ex.message}}")
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
        val expiryDateString = parseEntryString(entryObject, fieldNames.expiryDate)
        val ign = parseEntryString(entryObject, fieldNames.ign)
        val lengthOfBan = parseEntryString(entryObject, fieldNames.lengthOfBan)
        val reason = parseEntryString(entryObject, fieldNames.reason)
        val uuidString = parseEntryString(entryObject, fieldNames.uuid)

        // Parse the UUID from the string version
        val uuid = try {
            UUID.fromString(uuidString)
        } catch (ex: IllegalArgumentException) {
            throw InvalidDocumentFormatException("Invalid uuid - $uuidString")
        }

        // Parse date from the string version
        val dateExpires = try {
            dateFormat.parse(expiryDateString)
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
                    else -> return value
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