package com.androidtech.base.model

import androidx.annotation.DrawableRes
import com.androidtech.base.R

data class LanguageModel(
    val languageCode: String,
    val countryCode: String,
    val displayName: String,
    @get:DrawableRes
    val flagRes: Int,
    val isSelected: Boolean = false
) {
    companion object {
        val ENGLISH = LanguageModel("en", "US", "English", R.drawable.flag_united_states)
        val ENGLISH_AU = LanguageModel("en-AU", "AU", "English (Australia)", R.drawable.flag_australia)
        val ENGLISH_CA = LanguageModel("en-CA", "CA", "English (Canada)", R.drawable.flag_canada)
        val ENGLISH_GB = LanguageModel("en-GB", "GB", "English (United Kingdom)", R.drawable.flag_united_kingdom)
        val ENGLISH_IN = LanguageModel("en-IN", "IN", "English (India)", R.drawable.flag_india)
        val ARABIC = LanguageModel("ar", "SA", "العربية", R.drawable.flag_saudi_arabia)
        val AZERBAIJANI = LanguageModel("az", "AZ", "Azərbaycanca", R.drawable.flag_azerbaijan)
        val BULGARIAN = LanguageModel("bg", "BG", "Български", R.drawable.flag_bulgaria)
        val BENGALI = LanguageModel("bn", "BD", "বাংলা", R.drawable.flag_bangladesh)
        val CATALAN = LanguageModel("ca", "ES", "Català", R.drawable.flag_spain)
        val CZECH = LanguageModel("cs", "CZ", "Čeština", R.drawable.flag_czech_republic)
        val DENMARK = LanguageModel("da", "DK", "Dansk", R.drawable.flag_denmark)
        val GREEK = LanguageModel("el", "GR", "Ελληνικά", R.drawable.flag_greece)
        val SPANISH = LanguageModel("es", "ES", "Español", R.drawable.flag_spain)
        val SPANISH_MX = LanguageModel("es-MX", "MX", "Español (México)", R.drawable.flag_mexico)
        val SPANISH_US = LanguageModel("es-US", "US", "Español (Estados Unidos)", R.drawable.flag_united_states)
        val ESTONIAN = LanguageModel("et", "EE", "Eesti", R.drawable.flag_estonia)
        val PERSIAN = LanguageModel("fa", "IR", "فارسی", R.drawable.flag_iran)
        val FINNISH = LanguageModel("fi", "FI", "Suomi", R.drawable.flag_finland)
        val FRENCH = LanguageModel("fr", "FR", "Français", R.drawable.flag_france)
        val FRENCH_CA = LanguageModel("fr-CA", "CA", "Français (Canada)", R.drawable.flag_canada)
        val GUARANI = LanguageModel("gn", "PY", "Guaraní", R.drawable.flag_paraguay)
        val GUJARATI = LanguageModel("gu", "IN", "ગુજરાતી", R.drawable.flag_india)
        val HINDI = LanguageModel("hi", "IN", "हिन्दी", R.drawable.flag_india)
        val CROATIAN = LanguageModel("hr", "HR", "Hrvatski", R.drawable.flag_croatia)
        val HUNGARIAN = LanguageModel("hu", "HU", "Magyar", R.drawable.flag_hungary)
        val INDONESIAN = LanguageModel("in", "ID", "Indonesia", R.drawable.flag_indonesia)
        val ICELANDIC = LanguageModel("is", "IS", "Íslenska", R.drawable.flag_iceland)
        val ITALIAN = LanguageModel("it", "IT", "Italiano", R.drawable.flag_italy)
        val HEBREW = LanguageModel("iw", "IL", "עברית", R.drawable.flag_israel)
        val JAPAN = LanguageModel("ja", "JP", "日本語", R.drawable.flag_japan)
        val KANNADA = LanguageModel("kn", "IN", "ಕನ್ನಡ", R.drawable.flag_india)
        val KOREAN = LanguageModel("ko", "KR", "한국어", R.drawable.flag_south_korea)
        val LITHUANIAN = LanguageModel("lt", "LT", "Lietuvių", R.drawable.flag_lithuania)
        val MALAYALAM = LanguageModel("ml", "IN", "മലയാളം", R.drawable.flag_india)
        val MARATHI = LanguageModel("mr", "IN", "मराठी", R.drawable.flag_india)
        val MALAY = LanguageModel("ms", "MY", "Bahasa Melayu", R.drawable.flag_malaysia)
        val BURMESE = LanguageModel("my", "MM", "မြန်မာစာ", R.drawable.flag_myanmar)
        val DUTCH = LanguageModel("nl", "NL", "Nederlands", R.drawable.flag_netherlands)
        val POLISH = LanguageModel("pl", "PL", "Polski", R.drawable.flag_poland)
        val PORTUGUESE = LanguageModel("pt", "PT", "Português", R.drawable.flag_portugal)
        val PORTUGUESE_BRAZIL = LanguageModel("pt-BR", "BR", "Português (Brasil)", R.drawable.flag_brazil)
        val ROMANIAN = LanguageModel("ro", "RO", "Română", R.drawable.flag_romania)
        val SLOVAK = LanguageModel("sk", "SK", "Slovenčina", R.drawable.flag_slovakia)
        val SLOVENIAN = LanguageModel("sl", "SI", "Slovenščina", R.drawable.flag_slovenia)
        val SERBIAN = LanguageModel("sr", "RS", "Српски", R.drawable.flag_serbia)
        val SWEDISH = LanguageModel("sv", "SE", "Svenska", R.drawable.flag_sweden)
        val THAI = LanguageModel("th", "TH", "ไทย", R.drawable.flag_thailand)
        val TURKISH = LanguageModel("tr", "TR", "Türkçe", R.drawable.flag_turkey)
        val UKRAINA = LanguageModel("uk", "UA", "Українська", R.drawable.flag_ukraine)
        val UZBEK = LanguageModel("uz", "UZ", "Oʻzbekcha", R.drawable.flag_uzbekistan)
        val VIETNAMESE = LanguageModel("vi", "VN", "Tiếng Việt", R.drawable.flag_vietnam)
        val CHINA = LanguageModel("zh", "CN", "简体中文", R.drawable.flag_china)
        val CHINA_HONGKONG = LanguageModel("zh-HK", "HK", "繁體中文 (香港)", R.drawable.flag_hong_kong)
        val CHINA_SINGAPORE = LanguageModel("zh-SG", "SG", "简体中文 (新加坡)", R.drawable.flag_singapore)
        val CHINA_TAIWAN = LanguageModel("zh-TW", "TW", "繁體中文 (台灣)", R.drawable.flag_taiwan)

        val allLanguages: List<LanguageModel> = listOf(
            ENGLISH, ENGLISH_AU, ENGLISH_CA, ENGLISH_GB, ENGLISH_IN,
            ARABIC, AZERBAIJANI, BULGARIAN, BENGALI, CATALAN, CZECH, DENMARK, GREEK,
            SPANISH, SPANISH_MX, SPANISH_US,
            ESTONIAN, PERSIAN, FINNISH, FRENCH, FRENCH_CA,
            GUARANI, GUJARATI, HINDI, CROATIAN, HUNGARIAN, INDONESIAN, ICELANDIC, ITALIAN, HEBREW,
            JAPAN, KANNADA, KOREAN, LITHUANIAN, MALAYALAM, MARATHI, MALAY, BURMESE, DUTCH, POLISH,
            PORTUGUESE, PORTUGUESE_BRAZIL,
            ROMANIAN, SLOVAK, SLOVENIAN, SERBIAN, SWEDISH, THAI, TURKISH, UKRAINA, UZBEK, VIETNAMESE,
            CHINA, CHINA_HONGKONG, CHINA_SINGAPORE, CHINA_TAIWAN
        )

        fun getSupportedLanguageByCode(languageCode: String): LanguageModel? {
            return allLanguages.find { it.languageCode == languageCode }
        }
    }
}
