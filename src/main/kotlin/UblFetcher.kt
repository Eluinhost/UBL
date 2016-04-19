package gg.uhc.ubl

interface UblFetcher {
    open fun fetchAllRecords() : List<UblEntry>
}
