package com.example.data

data class ReciterCategory(
    val id: String,
    val displayName: String,
    val trackCount: Int,
    val keywords: List<String>
)

object ReciterCategorizer {

    // Known popular Quran reciters and their search keywords
    private val KNOWN_RECITERS = listOf(
        Pair("المنشاوي", listOf("المنشاوي", "منشاوي", "menshawy", "minshawi")),
        Pair("عبد الباسط", listOf("عبد الباسط", "عبدالباسط", "عبد الصمد", "abdulbasit", "abdelbasset")),
        Pair("الحصري", listOf("الحصري", "حصري", "hossary", "husary")),
        Pair("العفاسي", listOf("العفاسي", "عفاسي", "afasy", "alafasy")),
        Pair("المعيقلي", listOf("المعيقلي", "معيقلي", "mueaqly", "al-muaiqly")),
        Pair("السديس", listOf("السديس", "سديس", "sudais", "alsudaes")),
        Pair("الشاطري", listOf("الشاطري", "شاطري", "shatri", "al-shatri")),
        Pair("العجمي", listOf("العجمي", "عجمي", "ajmy", "alajmi")),
        Pair("الدوسري", listOf("الدوسري", "دوسري", "dossary", "al-dawsari")),
        Pair("الغامدي", listOf("الغامدي", "غامدي", "ghamdi", "alghamdi")),
        Pair("خالد الجليل", listOf("الجليل", "جليل", "jaleel")),
        Pair("إدريس أبكر", listOf("أبكر", "ابكر", "abkar")),
        Pair("فارس عباد", listOf("عباد", "abbad")),
        Pair("هزاع البلوشي", listOf("البلوشي", "بلوشي", "balushi")),
        Pair("علي جابر", listOf("علي جابر", "جابر", "ali jaber")),
        Pair("سعود الشريم", listOf("الشريم", "شريم", "shuraim")),
        Pair("مصطفى إسماعيل", listOf("مصطفى إسماعيل", "مصطفى اسماعيل", "mustafa ismail")),
        Pair("محمد رفعت", listOf("محمد رفعت", "رفعت", "refat", "rifat")),
        Pair("الطبلاوي", listOf("الطبلاوي", "طبلاوي", "tablawi")),
        Pair("محمود علي البنا", listOf("البنا", "بنا", "al-banna")),
        Pair("كامل البهتيمي", listOf("البهتيمي", "بهتيمي", "bahtimi")),
        Pair("محمد جبريل", listOf("جبريل", "jebril", "jibreel")),
        Pair("وديع اليمني", listOf("وديع اليمني", "وديع", "wadih"))
    )

    /**
     * Categorizes a list of tracks into smart reciter tabs.
     * Returns a list of categories starting with "الكل" (All).
     * 100% lightweight in-memory filtering.
     */
    fun categorize(tracks: List<AudioTrack>): List<ReciterCategory> {
        if (tracks.isEmpty()) return emptyList()

        val categories = mutableListOf<ReciterCategory>()

        // 1. "الكل" (All) category
        categories.add(
            ReciterCategory(
                id = "all",
                displayName = "الكل",
                trackCount = tracks.size,
                keywords = emptyList()
            )
        )

        // 2. Check for known reciters
        val matchedKnownReciters = mutableSetOf<String>()
        for ((reciterName, keywords) in KNOWN_RECITERS) {
            val matchingCount = tracks.count { track ->
                matchesKeywords(track, keywords)
            }
            if (matchingCount > 0) {
                categories.add(
                    ReciterCategory(
                        id = "reciter_${reciterName.hashCode()}",
                        displayName = reciterName,
                        trackCount = matchingCount,
                        keywords = keywords
                    )
                )
                matchedKnownReciters.add(reciterName)
            }
        }

        // 3. Dynamic subfolder detection (e.g. tracks organized in specific reciter folders)
        val folderGroups = tracks.groupBy { track ->
            val parentName = java.io.File(track.filePath).parentFile?.name
            if (parentName != null && parentName !in IGNORED_FOLDER_NAMES) parentName else null
        }

        for ((folderName, folderTracks) in folderGroups) {
            if (folderName != null && folderTracks.size >= 2) {
                // If folder wasn't already covered by a known reciter
                val alreadyCovered = categories.any { it.displayName.contains(folderName) || folderName.contains(it.displayName) }
                if (!alreadyCovered) {
                    categories.add(
                        ReciterCategory(
                            id = "folder_${folderName.hashCode()}",
                            displayName = folderName,
                            trackCount = folderTracks.size,
                            keywords = listOf(folderName.lowercase())
                        )
                    )
                }
            }
        }

        return categories
    }

    /**
     * Checks if a track belongs to a given category.
     */
    fun isTrackInCategory(track: AudioTrack, category: ReciterCategory): Boolean {
        if (category.id == "all" || category.keywords.isEmpty()) return true
        return matchesKeywords(track, category.keywords)
    }

    private fun matchesKeywords(track: AudioTrack, keywords: List<String>): Boolean {
        val targetText = "${track.fileName} ${track.title} ${track.reciterOrSubtitle} ${track.filePath}".lowercase()
        return keywords.any { kw -> targetText.contains(kw.lowercase()) }
    }

    private val IGNORED_FOLDER_NAMES = setOf(
        "Download", "download", "Music", "music", "Movies", "movies",
        "Documents", "documents", "sdcard", "0", "emulated", "storage"
    )
}
