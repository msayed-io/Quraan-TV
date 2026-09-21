package com.example.data

data class ReciterCategory(
    val id: String,
    val displayName: String,
    val trackCount: Int,
    val keywords: List<String> = emptyList()
)

/**
 * Dynamic Reciter Categorizer (Zero Hardcoded Names)
 *
 * 1. Dynamic Reciter Extraction (File Name Splitter):
 *    - Strips file extension and leading track numbers (e.g. "001 - ", "01_").
 *    - If the file name contains separators (-, _, –, —, |), splits and takes the first part as reciter.
 *    - If no separators exist, takes the first two words of the file name.
 *
 * 2. Distinct Dynamic Tabs:
 *    - Uses .distinct() on extracted names to eliminate duplicate reciter capsules.
 *    - Prepends a permanent "الكل" (All) tab.
 *    - Generates dynamic tabs purely from real scanned audio files in memory.
 *
 * 3. Extreme Efficiency (1GB RAM friendly):
 *    - Pure lightweight string operations, zero heavy regex loops or background services.
 */
object ReciterCategorizer {

    const val ALL_CATEGORY_ID = "all"

    /**
     * Flexibly extracts the sheikh / reciter name from the actual audio file name.
     * Absolutely NO hardcoded sheikh names.
     */
    fun extractReciterName(rawFileName: String): String {
        // 1. Remove file extension
        val nameWithoutExt = rawFileName.substringBeforeLast('.').trim()
        if (nameWithoutExt.isBlank()) return "أخرى"

        // 2. Strip leading track numbers like "001 - ", "01_", "1- ", "01. ", "001 "
        val withoutLeadingNum = nameWithoutExt
            .replaceFirst(Regex("^[0-9٠-٩]+[\\s._\\-–—|~]+"), "")
            .trim()
        val workingName = if (withoutLeadingNum.isNotBlank()) withoutLeadingNum else nameWithoutExt

        // 3. Check for explicit delimiters: '-', '_', '–', '—', '|'
        val separatorRegex = Regex("[-_–—|]")
        val hasSeparator = workingName.contains(separatorRegex)

        val reciterPart = if (hasSeparator) {
            // Split by separator and take the first part
            val parts = workingName.split(separatorRegex)
                .map { it.trim() }
                .filter { it.isNotBlank() }
            if (parts.isNotEmpty()) {
                parts[0]
            } else {
                workingName
            }
        } else {
            // If no separators, take the first two words from the file name
            val words = workingName.split(Regex("\\s+")).filter { it.isNotBlank() }
            when {
                words.size >= 2 -> "${words[0]} ${words[1]}"
                words.size == 1 -> words[0]
                else -> workingName
            }
        }

        // Clean any punctuation artifacts from borders
        val cleaned = reciterPart
            .trim()
            .trim('-', '_', '–', '—', '|', '.', ':', ';', '(', ')', '[', ']', '{', '}')
            .trim()

        return if (cleaned.isNotBlank() && !cleaned.all { it.isDigit() }) {
            cleaned
        } else {
            "أخرى"
        }
    }

    /**
     * Categorizes a list of tracks into distinct smart reciter tabs dynamically.
     * Returns a list starting with the default "الكل" (All) tab,
     * followed by unique tabs generated from extracted reciter names.
     */
    fun categorize(tracks: List<AudioTrack>): List<ReciterCategory> {
        if (tracks.isEmpty()) return emptyList()

        val categories = mutableListOf<ReciterCategory>()

        // 1. Permanent Default Tab: "الكل" (All) showing all files without filtering
        categories.add(
            ReciterCategory(
                id = ALL_CATEGORY_ID,
                displayName = "الكل",
                trackCount = tracks.size
            )
        )

        // 2. Extract reciter names dynamically and collect distinct sheikh names
        val distinctReciterNames = tracks
            .map { it.reciterName.ifBlank { extractReciterName(it.fileName) } }
            .filter { it.isNotBlank() && it != "أخرى" }
            .distinct()

        // 3. Build unique category tabs with calculated counts
        for (reciterName in distinctReciterNames) {
            val count = tracks.count { track ->
                val r = track.reciterName.ifBlank { extractReciterName(track.fileName) }
                r.equals(reciterName, ignoreCase = true)
            }
            if (count > 0) {
                categories.add(
                    ReciterCategory(
                        id = "reciter_${reciterName.hashCode()}",
                        displayName = reciterName,
                        trackCount = count
                    )
                )
            }
        }

        // 4. If any tracks had no identifiable name and fallback to "أخرى"
        val otherCount = tracks.count { track ->
            val r = track.reciterName.ifBlank { extractReciterName(track.fileName) }
            r == "أخرى"
        }
        if (otherCount > 0 && distinctReciterNames.isNotEmpty()) {
            categories.add(
                ReciterCategory(
                    id = "reciter_other",
                    displayName = "أخرى",
                    trackCount = otherCount
                )
            )
        }

        return categories
    }

    /**
     * Checks if a track belongs to a given category.
     * O(1) comparison matching the dynamic reciter name.
     */
    fun isTrackInCategory(track: AudioTrack, category: ReciterCategory): Boolean {
        if (category.id == ALL_CATEGORY_ID) return true
        val trackReciter = track.reciterName.ifBlank { extractReciterName(track.fileName) }
        return trackReciter.equals(category.displayName, ignoreCase = true)
    }
}
