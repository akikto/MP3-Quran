package com.example.model

data class FamousAyah(
    val surahNumber: Int,
    val ayahNumber: Int,
    val name: String,
    val arabicText: String,
    val englishTranslation: String
) {
    companion object {
        val POPULAR_AYAHS: List<FamousAyah> = listOf(
            FamousAyah(
                surahNumber = 2,
                ayahNumber = 255,
                name = "Ayat al-Kursi (The Throne Verse)",
                arabicText = "اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ ۚ لَا تَأْخُذُهُ سِنَةٌ وَلَا نَوْمٌ",
                englishTranslation = "Allah! There is no deity except Him, the Ever-Living, the Sustainer of all existence."
            ),
            FamousAyah(
                surahNumber = 2,
                ayahNumber = 286,
                name = "Last Verse of Al-Baqarah (Amanar-Rasul)",
                arabicText = "لَا يُكَلِّفُ اللَّهُ نَفْسًا إِلَّا وُسْعَهَا ۚ لَهَا مَا كَسَبَتْ وَعَلَيْهَا مَا اكْتَسَبَتْ",
                englishTranslation = "Allah does not burden a soul beyond that it can bear. It will have [the consequence of] what [good] it has gained."
            ),
            FamousAyah(
                surahNumber = 3,
                ayahNumber = 173,
                name = "Hasbunallahu Wa Ni'mal Wakeel",
                arabicText = "حَسْبُنَا اللَّهُ وَنِعْمَ الْوَكِيلُ",
                englishTranslation = "Sufficient for us is Allah, and [He is] the best Disposer of affairs."
            ),
            FamousAyah(
                surahNumber = 18,
                ayahNumber = 10,
                name = "Dua of the Companions of the Cave",
                arabicText = "رَبَّنَا آتِنَا مِن لَّدُنكَ رَحْمَةً وَهَيِّئْ لَنَا مِنْ أَمْرِنَا رَشَدًا",
                englishTranslation = "Our Lord, grant us from Yourself mercy and prepare for us from our affair right guidance."
            ),
            FamousAyah(
                surahNumber = 20,
                ayahNumber = 25,
                name = "Musa's Dua for Speech and Ease",
                arabicText = "رَبِّ اشْرَحْ لِي صَدْرِي وَيَسِّرْ لِي أَمْرِي",
                englishTranslation = "My Lord, expand for me my breast [with assurance] and ease for me my task."
            ),
            FamousAyah(
                surahNumber = 21,
                ayahNumber = 87,
                name = "Dua of Prophet Yunus (Dhun-Nun)",
                arabicText = "لَّا إِلَٰهَ إِلَّا أَنتَ سُبْحَانَكَ إِنِّي كُنتُ مِنَ الظَّالِمِينَ",
                englishTranslation = "There is no deity except You; exalted are You. Indeed, I have been of the wrongdoers."
            ),
            FamousAyah(
                surahNumber = 94,
                ayahNumber = 6,
                name = "Ease with Hardship",
                arabicText = "إِنَّ مَعَ الْعُسْرِ يُسْرًا",
                englishTranslation = "Indeed, with hardship [will be] ease."
            ),
            FamousAyah(
                surahNumber = 112,
                ayahNumber = 1,
                name = "Al-Ikhlas (Tawheed)",
                arabicText = "قُلْ هُوَ اللَّهُ أَحَدٌ • اللَّهُ الصَّمَدُ",
                englishTranslation = "Say, 'He is Allah, [who is] One, Allah, the Eternal Refuge.'"
            )
        )
    }
}

data class AyahBookmark(
    val id: Long = 0,
    val surah: Surah,
    val ayahNumber: Int,
    val note: String = "",
    val timestampMs: Long = 0L,
    val bookmarkedAt: Long = System.currentTimeMillis()
)
