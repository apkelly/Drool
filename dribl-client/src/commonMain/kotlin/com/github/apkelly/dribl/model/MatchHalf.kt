package com.github.apkelly.dribl.model

enum class MatchHalf(
    val apiValue: String,
) {
    FirstHalf("ft-first-half"),
    SecondHalf("ft-second-half"),
    ;

    companion object {
        fun fromApiValue(
            value: String?,
        ): MatchHalf = entries.firstOrNull {
            it.apiValue.equals(value, ignoreCase = true)
        } ?: FirstHalf
    }
}
