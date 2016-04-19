package gg.uhc.ubl

import com.google.common.io.Resources
import org.assertj.core.api.Assertions.assertThat
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import org.junit.Before
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.logging.Logger


class GoogleSpreadsheetUblFetcherTest {

    lateinit var fetcher: GoogleSpreadsheetUblFetcher
    val fieldNames = GoogleSpreadsheetUblFetcher.GoogleSpreadSheetColumnNames(
            caseUrl = "case",
            expiryDate = "expirydate",
            dateBanned = "datebanned",
            ign = "ign",
            lengthOfBan = "lengthofban",
            reason = "reason",
            uuid = "uuid"
    )

    @Before fun startup() {
        fetcher = GoogleSpreadsheetUblFetcher(
            documentId = "",
            worksheetId = "",
            dateFormat = SimpleDateFormat("MMMMMddyyyy"),
            fieldNames = fieldNames,
            headerRows = 1,
            logger = Logger.getAnonymousLogger()
        )
    }


    @Test fun test_parse_outer_object_valid() {
        fetcher.parseOuterObject("{}")
        fetcher.parseOuterObject("""{"test":"value"}""")
    }

    @Test(expected = GoogleSpreadsheetUblFetcher.InvalidDocumentFormatException::class)
    fun test_parse_outer_object_empty() {
        fetcher.parseOuterObject("")
    }

    @Test(expected = GoogleSpreadsheetUblFetcher.InvalidDocumentFormatException::class)
    fun test_parse_outer_object_invalid() {
        fetcher.parseOuterObject("[]")
    }

    @Test fun test_grab_entries_list() {
        val noEntries = JSONParser().parse("""
        {
            "feed": {
                "entry": []
            }
        }
        """)
        val threeEntries = JSONParser().parse("""
        {
            "feed": {
                "entry": [
                    "one", "two", "three"
                ]
            }
        }
        """)


        assertThat(fetcher.grabEntriesList(noEntries as JSONObject)).hasSize(0)
        assertThat(fetcher.grabEntriesList(threeEntries as JSONObject)).hasSize(3)
    }

    @Test(expected = GoogleSpreadsheetUblFetcher.InvalidDocumentFormatException::class)
    fun test_grab_entries_missing_feed() {
        val missingFeed = JSONParser().parse("""
        {
            "entry": []
        }
        """)

        fetcher.grabEntriesList(missingFeed as JSONObject)
    }

    @Test(expected = GoogleSpreadsheetUblFetcher.InvalidDocumentFormatException::class)
    fun test_grab_entries_invalid_feed() {
        val invalidFeed = JSONParser().parse("""
        {
            "feed": []
        }
        """)

        fetcher.grabEntriesList(invalidFeed as JSONObject)
    }

    @Test(expected = GoogleSpreadsheetUblFetcher.InvalidDocumentFormatException::class)
    fun test_grab_entries_missing_entry() {
        val missingEntry = JSONParser().parse("""
        {
            "feed": {}
        }
        """)

        fetcher.grabEntriesList(missingEntry as JSONObject)
    }

    @Test(expected = GoogleSpreadsheetUblFetcher.InvalidDocumentFormatException::class)
    fun test_grab_entries_invalid_entry() {
        val invalidEntry = JSONParser().parse("""
        {
            "feed": {
                "entry": {}
            }
        }
        """)

        fetcher.grabEntriesList(invalidEntry as JSONObject)
    }

    @Test
    fun test_parse_entry_string() {
        val valid = JSONParser().parse("""
        {
            "gsx${'$'}test": {
                "${'$'}t": "value"
            }
        }
        """)

        assertThat(fetcher.parseEntryString(valid as JSONObject, "test")).isEqualTo("value")
    }

    @Test(expected = GoogleSpreadsheetUblFetcher.InvalidDocumentFormatException::class)
    fun test_parse_entry_string_missing_key() {
        val missingKey = JSONParser().parse("{}")

        fetcher.parseEntryString(missingKey as JSONObject, "test");
    }

    @Test(expected = GoogleSpreadsheetUblFetcher.InvalidDocumentFormatException::class)
    fun test_parse_entry_string_invalid_key() {
        val invalidKey = JSONParser().parse("""
        {
            "gsx${'$'}test": ["invalid"]
        }
        """)

        fetcher.parseEntryString(invalidKey as JSONObject, "test");
    }

    @Test(expected = GoogleSpreadsheetUblFetcher.InvalidDocumentFormatException::class)
    fun test_parse_entry_string_invalid_subkey() {
        val invalidKey = JSONParser().parse("""
        {
            "gsx${'$'}test": {
                "${'$'}t": ["invalid"]
            }
        }
        """)

        fetcher.parseEntryString(invalidKey as JSONObject, "test");
    }

    @Test(expected = GoogleSpreadsheetUblFetcher.InvalidDocumentFormatException::class)
    fun test_parse_entry_string_missing_subkey() {
        val invalidKey = JSONParser().parse("""
        {
            "gsx${'$'}test": {}
        }
        """)

        fetcher.parseEntryString(invalidKey as JSONObject, "test");
    }

    @Test
    fun test_parse_entry() {
        val entryJSON = JSONParser().parse("""
        {
            "gsx${'$'}${fieldNames.caseUrl}": {
                "${'$'}t": "${fieldNames.caseUrl}"
            },
            "gsx${'$'}${fieldNames.ign}": {
                "${'$'}t": "${fieldNames.ign}"
            },
            "gsx${'$'}${fieldNames.lengthOfBan}": {
                "${'$'}t": "${fieldNames.lengthOfBan}"
            },
            "gsx${'$'}${fieldNames.reason}": {
                "${'$'}t": "${fieldNames.reason}"
            },
            "gsx${'$'}${fieldNames.uuid}": {
                "${'$'}t": "c0b075fa-049d-49ec-879e-45e5f0e66b08"
            },
            "gsx${'$'}${fieldNames.dateBanned}": {
                "${'$'}t": "February 17, 2015"
            },
            "gsx${'$'}${fieldNames.expiryDate}": {
                "${'$'}t": "February 17, 2015"
            }
        }
        """)

        val entry = fetcher.parseEntry(entryJSON as JSONObject)

        assertThat(entry.caseUrl).isEqualTo(fieldNames.caseUrl)
        assertThat(entry.ign).isEqualTo(fieldNames.ign)
        assertThat(entry.lengthOfBan).isEqualTo(fieldNames.lengthOfBan)
        assertThat(entry.reason).isEqualTo(fieldNames.reason)
        assertThat(entry.uuid.toString()).isEqualTo("c0b075fa-049d-49ec-879e-45e5f0e66b08")
        assertThat(entry.expires).isWithinDayOfMonth(17)
        assertThat(entry.expires).isWithinMonth(2)
        assertThat(entry.expires).isWithinYear(2015)
    }

    @Test
    fun test_parse_valid_entries() {
        val entriesJSON = JSONParser().parse("""
        [
            { "header": "row"},
            {
                "gsx${'$'}${fieldNames.caseUrl}": {
                    "${'$'}t": "${fieldNames.caseUrl}"
                },
                "gsx${'$'}${fieldNames.ign}": {
                    "${'$'}t": "${fieldNames.ign}"
                },
                "gsx${'$'}${fieldNames.lengthOfBan}": {
                    "${'$'}t": "${fieldNames.lengthOfBan}"
                },
                "gsx${'$'}${fieldNames.reason}": {
                    "${'$'}t": "${fieldNames.reason}"
                },
                "gsx${'$'}${fieldNames.uuid}": {
                    "${'$'}t": "c0b075fa-049d-49ec-879e-45e5f0e66b08"
                },
                "gsx${'$'}${fieldNames.dateBanned}": {
                    "${'$'}t": "February 17, 2015"
                },
                "gsx${'$'}${fieldNames.expiryDate}": {
                    "${'$'}t": "February 17, 2015"
                }
            },
            {
                "invalid entry": "value"
            },
            {
                "gsx${'$'}${fieldNames.caseUrl}": {
                    "${'$'}t": "${fieldNames.caseUrl}"
                },
                "gsx${'$'}${fieldNames.ign}": {
                    "${'$'}t": "${fieldNames.ign}"
                },
                "gsx${'$'}${fieldNames.lengthOfBan}": {
                    "${'$'}t": "${fieldNames.lengthOfBan}"
                },
                "gsx${'$'}${fieldNames.reason}": {
                    "${'$'}t": "${fieldNames.reason}"
                },
                "gsx${'$'}${fieldNames.uuid}": {
                    "${'$'}t": "c0b075fa-049d-49ec-879e-45e5f0e66b08"
                },
                "gsx${'$'}${fieldNames.dateBanned}": {
                    "${'$'}t": "February 17, 2015"
                },
                "gsx${'$'}${fieldNames.expiryDate}": {
                    "${'$'}t": "February 17, 2015"
                }
            }
        ]
        """)

        val entries = fetcher.convertEntryList(entriesJSON as JSONArray)

        assertThat(entries).hasSize(2)
    }

    @Test
    fun test_on_live_data() {
        val entries = fetcher.processRawJSON(Resources.toString(Resources.getResource("liveData.json"), Charsets.UTF_8))

        assertThat(entries).hasSize(2045)
    }
}