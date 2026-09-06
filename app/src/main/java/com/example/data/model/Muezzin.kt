package com.example.data.model

enum class Muezzin(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val audioUrl: String,
    val descriptionAr: String,
    val descriptionEn: String
) {
    OSAMA_AL_KARBALAI(
        id = "osama_karbalai",
        nameAr = "أسامة الكربلائي",
        nameEn = "Osama Al-Karbalai",
        audioUrl = "https://raw.githubusercontent.com/moshraheem-sudo/Muezzin/main/%D8%A3%D8%B3%D8%A7%D9%85%D8%A9%20%D8%A7%D9%84%D9%83%D8%B1%D8%A8%D9%84%D8%A7%D8%A6%D9%8A%20.mp3",
        descriptionAr = "مؤذن العتبتين المقدستين الحسينية والعباسية",
        descriptionEn = "Holy Karbala Shrines Muezzin"
    ),
    RAFEA_AL_AMIRI(
        id = "rafea_amiri",
        nameAr = "رافع العامري",
        nameEn = "Rafea Al-Amiri",
        audioUrl = "https://raw.githubusercontent.com/moshraheem-sudo/Muezzin/main/%D8%B1%D8%A7%D9%81%D8%B9%20%D8%A7%D9%84%D8%B9%D8%A7%D9%85%D8%B1%D9%8A%20.mp3",
        descriptionAr = "المقرئ والمؤذن العراقي المعروف",
        descriptionEn = "Renowned Iraqi Qari & Muezzin"
    ),
    AMER_AL_KADHIMI(
        id = "amer_kadhimi",
        nameAr = "عامر الكاظمي",
        nameEn = "Amer Al-Kadhimi",
        audioUrl = "https://raw.githubusercontent.com/moshraheem-sudo/Muezzin/main/%D8%B9%D8%A7%D9%85%D8%B1%20%D8%A7%D9%84%D9%83%D8%A7%D8%B8%D9%85%D9%8A%20.mp3",
        descriptionAr = "مؤذن العتبة الكاظمية المقدسة",
        descriptionEn = "Holy Kadhimiya Shrine Muezzin"
    );

    companion object {
        val defaultMuezzin = OSAMA_AL_KARBALAI

        fun fromId(id: String?): Muezzin {
            return values().firstOrNull { it.id == id } ?: defaultMuezzin
        }
    }
}
