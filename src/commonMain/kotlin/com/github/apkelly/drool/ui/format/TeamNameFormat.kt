package com.github.apkelly.drool.ui.format

import com.github.apkelly.drool.domain.model.Fixture

data class FixtureTeamNames(
    val home: String,
    val away: String,
)

fun Fixture.displayTeamNames(): FixtureTeamNames =
    displayTeamNames(
        homeName = homeTeamName,
        awayName = awayTeamName,
        namingContexts = listOfNotNull(competitionName, leagueName),
    )

internal fun displayTeamNames(
    homeName: String,
    awayName: String,
    namingContexts: List<String>,
): FixtureTeamNames {
    val original = FixtureTeamNames(homeName, awayName)
    val contextWords = namingContexts
        .flatMap { context -> teamNameWord.findAll(context).map { it.normalizedValue() } }
        .toSet()
    if (contextWords.isEmpty()) return original

    val homeWords = teamNameWord.findAll(homeName).toList()
    val awayWords = teamNameWord.findAll(awayName).toList()
    val commonWordCount = homeWords.zip(awayWords)
        .takeWhile { (home, away) -> home.value.equals(away.value, ignoreCase = true) }
        .size
    if (commonWordCount == 0) return original

    val removedIndices = (0 until commonWordCount).filter { index ->
        homeWords[index].normalizedValue() in contextWords
    }.toSet()
    if (removedIndices.isEmpty()) return original

    val home = homeName.withoutSharedContextWords(homeWords, commonWordCount, removedIndices)
    val away = awayName.withoutSharedContextWords(awayWords, commonWordCount, removedIndices)
    return if (home.isNotEmpty() && away.isNotEmpty()) {
        FixtureTeamNames(home, away)
    } else {
        original
    }
}

private fun String.withoutSharedContextWords(
    words: List<MatchResult>,
    commonWordCount: Int,
    removedIndices: Set<Int>,
): String {
    val retainedPrefix = (0 until commonWordCount)
        .filterNot(removedIndices::contains)
        .joinToString(" ") { index -> words[index].value }
    val uniqueSuffix = words.getOrNull(commonWordCount)
        ?.let { word -> substring(word.range.first).trim() }
        .orEmpty()
    return listOf(retainedPrefix, uniqueSuffix)
        .filter(String::isNotBlank)
        .joinToString(" ")
}

private fun MatchResult.normalizedValue(): String = value.lowercase()
private val teamNameWord = Regex("""[\p{L}\p{N}]+(?:['’][\p{L}\p{N}]+)?""")
