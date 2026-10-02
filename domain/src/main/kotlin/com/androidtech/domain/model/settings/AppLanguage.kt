package com.androidtech.domain.model.settings

/**
 * User-selectable app language. [languageTag] is a BCP-47 tag suitable for
 * `LocaleListCompat.forLanguageTags(...)`; null for [SYSTEM], which means "don't override —
 * follow the OS/app default locale".
 */
enum class AppLanguage(
    val languageTag: String?,
    val nativeName: String,
    val englishName: String,
    val flagEmoji: String
) {
    SYSTEM(null, "System Default", "System Default", "⚙️"),
    ENGLISH("en", "English", "English", "🇺🇸"),
    VIETNAMESE("vi", "Tiếng Việt", "Vietnamese", "🇻🇳"),
    SPANISH("es", "Español", "Spanish", "🇪🇸"),
    FRENCH("fr", "Français", "French", "🇫🇷"),
    GERMAN("de", "Deutsch", "German", "🇩🇪"),
    JAPANESE("ja", "日本語", "Japanese", "🇯🇵"),
    KOREAN("ko", "한국어", "Korean", "🇰🇷"),
    CHINESE("zh", "中文 (简体)", "Chinese (Simplified)", "🇨🇳"),
    PORTUGUESE("pt", "Português", "Portuguese", "🇧🇷"),
    HINDI("hi", "हिन्दी", "Hindi", "🇮🇳"),
    ARABIC("ar", "العربية", "Arabic", "🇸🇦"),
    RUSSIAN("ru", "Русский", "Russian", "🇷🇺"),
    INDONESIAN("id", "Bahasa Indonesia", "Indonesian", "🇮🇩"),
    THAI("th", "ไทย", "Thai", "🇹🇭");

    val displayName: String
        get() = if (this == SYSTEM) nativeName else "$flagEmoji  $nativeName ($englishName)"

    companion object {
        fun fromLanguageTag(tag: String?): AppLanguage =
            entries.firstOrNull { it.languageTag == tag } ?: SYSTEM

        fun fromCode(code: String): AppLanguage =
            entries.firstOrNull { it.languageTag == code } ?: SYSTEM

        fun fromDisplayName(name: String): AppLanguage =
            entries.firstOrNull { it.displayName == name || it.nativeName == name } ?: SYSTEM
    }
}