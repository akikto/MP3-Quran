package com.example.model

data class Reciter(
    val id: String,
    val nameEnglish: String,
    val nameArabic: String,
    val style: String,
    val serverUrl: String,
    val country: String,
    val bio: String
) {
    fun getAudioUrl(surahNumber: Int): String {
        val formatted = "%03d".format(surahNumber)
        val base = if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/"
        return "$base$formatted.mp3"
    }

    companion object {
        val ALL_RECITERS = listOf(
            Reciter(
                id = "afs",
                nameEnglish = "Mishary Rashid Alafasy",
                nameArabic = "مشاري راشد العفاسي",
                style = "Hafs 'an 'Asim (Murattal)",
                serverUrl = "https://server8.mp3quran.net/afs/",
                country = "Kuwait",
                bio = "World-famous Imam of the Grand Mosque of Kuwait, celebrated for his melodious recitation."
            ),
            Reciter(
                id = "sds",
                nameEnglish = "Abdul Rahman Al-Sudais",
                nameArabic = "عبد الرحمن السديس",
                style = "Hafs 'an 'Asim (Murattal)",
                serverUrl = "https://server11.mp3quran.net/sds/",
                country = "Saudi Arabia",
                bio = "Chief Imam and President of the Affairs of the Two Holy Mosques in Mecca."
            ),
            Reciter(
                id = "maher",
                nameEnglish = "Maher Al-Muaiqly",
                nameArabic = "ماهر المعيقلي",
                style = "Hafs 'an 'Asim (Murattal)",
                serverUrl = "https://server12.mp3quran.net/maher/",
                country = "Saudi Arabia",
                bio = "Renowned Imam of Masjid al-Haram in Mecca, famous for his deep, soothing tone."
            ),
            Reciter(
                id = "ghamdi",
                nameEnglish = "Saad Al-Ghamdi",
                nameArabic = "سعد الغامدي",
                style = "Hafs 'an 'Asim (Murattal)",
                serverUrl = "https://server7.mp3quran.net/ghamdi/",
                country = "Saudi Arabia",
                bio = "Acclaimed Qari with crystal-clear pronunciation and emotional resonance."
            ),
            Reciter(
                id = "basit",
                nameEnglish = "Abdulbasit Abdulsamad",
                nameArabic = "عبد الباسط عبد الصمد",
                style = "Hafs 'an 'Asim (Murattal)",
                serverUrl = "https://server7.mp3quran.net/basit/",
                country = "Egypt",
                bio = "Golden voice of the Islamic world, one of the greatest Quran reciters in modern history."
            ),
            Reciter(
                id = "husr",
                nameEnglish = "Mahmoud Khalil Al-Husary",
                nameArabic = "محمود خليل الحصري",
                style = "Hafs 'an 'Asim (Murattal)",
                serverUrl = "https://server13.mp3quran.net/husr/",
                country = "Egypt",
                bio = "The master of Tajweed rules, respected universally as the reference standard for Quranic recitation."
            ),
            Reciter(
                id = "shatri",
                nameEnglish = "Abu Bakr Al-Shatri",
                nameArabic = "أبو بكر الشاطري",
                style = "Hafs 'an 'Asim (Murattal)",
                serverUrl = "https://server11.mp3quran.net/shatri/",
                country = "Saudi Arabia",
                bio = "Jeddah-based Qari known for his calm, measured and spiritually uplifting cadence."
            ),
            Reciter(
                id = "yasser",
                nameEnglish = "Yasser Al-Dosari",
                nameArabic = "ياسر الدوسري",
                style = "Hafs 'an 'Asim (Murattal)",
                serverUrl = "https://server11.mp3quran.net/yasser/",
                country = "Saudi Arabia",
                bio = "Imam at Masjid al-Haram, widely praised for his powerful and heartfelt recitations."
            ),
            Reciter(
                id = "shur",
                nameEnglish = "Saud Al-Shuraim",
                nameArabic = "سعود الشريم",
                style = "Hafs 'an 'Asim (Murattal)",
                serverUrl = "https://server7.mp3quran.net/shur/",
                country = "Saudi Arabia",
                bio = "Former prominent Imam and Khateeb of the Grand Mosque in Mecca."
            ),
            Reciter(
                id = "minsh",
                nameEnglish = "Muhammad Siddiq Al-Minshawi",
                nameArabic = "محمد صديق المنشاوي",
                style = "Hafs 'an 'Asim (Murattal)",
                serverUrl = "https://server10.mp3quran.net/minsh/",
                country = "Egypt",
                bio = "Legendary Egyptian Qari known for his weeping, profoundly sincere voice."
            ),
            Reciter(
                id = "hthfi",
                nameEnglish = "Ali Al-Hudhaify",
                nameArabic = "علي الحذيفي",
                style = "Hafs 'an 'Asim (Murattal)",
                serverUrl = "https://server9.mp3quran.net/hthfi/",
                country = "Saudi Arabia",
                bio = "Chief Imam of the Prophet's Mosque (Al-Masjid an-Nabawi) in Medina."
            ),
            Reciter(
                id = "qtm",
                nameEnglish = "Nasser Al-Qatami",
                nameArabic = "ناصر القطامي",
                style = "Hafs 'an 'Asim (Murattal)",
                serverUrl = "https://server6.mp3quran.net/qtm/",
                country = "Saudi Arabia",
                bio = "Imam at Princess Latifa Bint Sultan Mosque in Riyadh, admired for his melodious style."
            )
        )

        val DEFAULT_RECITER: Reciter = ALL_RECITERS[0] // Mishary Rashid Alafasy

        fun getById(id: String): Reciter = ALL_RECITERS.find { it.id == id } ?: DEFAULT_RECITER
    }
}
