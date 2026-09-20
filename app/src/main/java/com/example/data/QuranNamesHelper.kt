package com.example.data

object QuranNamesHelper {

    val SURAH_NAMES_ARABIC = mapOf(
        1 to "سورة الفاتحة",
        2 to "سورة البقرة",
        3 to "سورة آل عمران",
        4 to "سورة النساء",
        5 to "سورة المائدة",
        6 to "سورة الأنعام",
        7 to "سورة الأعراف",
        8 to "سورة الأنفال",
        9 to "سورة التوبة",
        10 to "سورة يونس",
        11 to "سورة هود",
        12 to "سورة يوسف",
        13 to "سورة الرعد",
        14 to "سورة إبراهيم",
        15 to "سورة الحجر",
        16 to "سورة النحل",
        17 to "سورة الإسراء",
        18 to "سورة الكهف",
        19 to "سورة مريم",
        20 to "سورة طه",
        21 to "سورة الأنبياء",
        22 to "سورة الحج",
        23 to "سورة المؤمنون",
        24 to "سورة النور",
        25 to "سورة الفرقان",
        26 to "سورة الشعراء",
        27 to "سورة النمل",
        28 to "سورة القصص",
        29 to "سورة العنكبوت",
        30 to "سورة الروم",
        31 to "سورة لقمان",
        32 to "سورة السجدة",
        33 to "سورة الأحزاب",
        34 to "سورة سبأ",
        35 to "سورة فاطر",
        36 to "سورة يس",
        37 to "سورة الصافات",
        38 to "سورة ص",
        39 to "سورة الزمر",
        40 to "سورة غافر",
        41 to "سورة فصلت",
        42 to "سورة الشورى",
        43 to "سورة الزخرف",
        44 to "سورة الدخان",
        45 to "سورة الجاثية",
        46 to "سورة الأحقاف",
        47 to "سورة محمد",
        48 to "سورة الفتح",
        49 to "سورة الحجرات",
        50 to "سورة ق",
        51 to "سورة الذاريات",
        52 to "سورة الطور",
        53 to "سورة النجم",
        54 to "سورة القمر",
        55 to "سورة الرحمن",
        56 to "سورة الواقعة",
        57 to "سورة الحديد",
        58 to "سورة المجادلة",
        59 to "سورة الحشر",
        60 to "سورة الممتحنة",
        61 to "سورة الصف",
        62 to "سورة الجمعة",
        63 to "سورة المنافقون",
        64 to "سورة التغابن",
        65 to "سورة الطلاق",
        66 to "سورة التحريم",
        67 to "سورة الملك",
        68 to "سورة القلم",
        69 to "سورة الحاقة",
        70 to "سورة المعارج",
        71 to "سورة نوح",
        72 to "سورة الجن",
        73 to "سورة المزمل",
        74 to "سورة المدثر",
        75 to "سورة القيامة",
        76 to "سورة الإنسان",
        77 to "سورة المرسلات",
        78 to "سورة النبأ",
        79 to "سورة النازعات",
        80 to "سورة عبس",
        81 to "سورة التكوير",
        82 to "سورة الانفطار",
        83 to "سورة المطففين",
        84 to "سورة الانشقاق",
        85 to "سورة البروج",
        86 to "سورة الطارق",
        87 to "سورة الأعلى",
        88 to "سورة الغاشية",
        89 to "سورة الفجر",
        90 to "سورة البلد",
        91 to "سورة الشمس",
        92 to "سورة الليل",
        93 to "سورة الضحى",
        94 to "سورة الشرح",
        95 to "سورة التين",
        96 to "سورة العلق",
        97 to "سورة القدر",
        98 to "سورة البينة",
        99 to "سورة الزلزلة",
        100 to "سورة العاديات",
        101 to "سورة القارعة",
        102 to "سورة التكاثر",
        103 to "سورة العصر",
        104 to "سورة الهمزة",
        105 to "سورة الفيل",
        106 to "سورة قريش",
        107 to "سورة الماعون",
        108 to "سورة الكوثر",
        109 to "سورة الكافرون",
        110 to "سورة النصر",
        111 to "سورة المسد",
        112 to "سورة الإخلاص",
        113 to "سورة الفلق",
        114 to "سورة الناس"
    )

    fun parseSurahDetails(rawFileName: String, metadataTitle: String?): Pair<String, String> {
        val cleanName = rawFileName.substringBeforeLast(".")
        val textToSearch = (metadataTitle ?: "") + " " + cleanName

        // Check for surah number 1..114
        val numberRegex = Regex("(?:^|\\D)(00[1-9]|0[1-9][0-9]|10[0-9]|11[0-4]|[1-9][0-9]?)(?:\\D|$)")
        val match = numberRegex.find(cleanName)
        val surahNum = match?.groupValues?.get(1)?.toIntOrNull()

        if (surahNum != null && SURAH_NAMES_ARABIC.containsKey(surahNum)) {
            val surahArabic = SURAH_NAMES_ARABIC[surahNum]!!
            val subtitle = metadataTitle?.takeIf { it.isNotBlank() && it != cleanName } ?: "تلاوة مباركة (رقم ${surahNum})"
            return Pair(surahArabic, subtitle)
        }

        // Match Arabic surah names directly
        for ((_, name) in SURAH_NAMES_ARABIC) {
            val shortName = name.removePrefix("سورة ")
            if (textToSearch.contains(shortName) || textToSearch.contains(name)) {
                return Pair(name, metadataTitle ?: cleanName)
            }
        }

        // Default display
        val title = if (!metadataTitle.isNullOrBlank()) metadataTitle else cleanName
        return Pair(title, "ملف صوتي من التنزيلات")
    }
}
