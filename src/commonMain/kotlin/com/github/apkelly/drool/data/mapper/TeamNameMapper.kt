package com.github.apkelly.drool.data.mapper

internal fun normalizedTeamName(
    name: String,
    competitionName: String?,
    allowEmbeddedLeaguePrefix: Boolean = false,
): String {
    val trimmedName = name.trim()
    val competition = competitionName?.trim().orEmpty()
    if (competition.isEmpty()) return trimmedName

    matchingPrefixEnd(trimmedName, competition)?.let { suffixStart ->
        return trimmedName.substring(suffixStart).trim().ifEmpty { trimmedName }
    }
    if (!allowEmbeddedLeaguePrefix) return trimmedName

    val nameWords = nonWhitespace.findAll(trimmedName).toList()
    val leagueWords = nonWhitespace.findAll(competition).map { it.value }.toList()
    if (leagueWords.size < MinimumEmbeddedLeagueWords) return trimmedName
    nameWords.forEachIndexed { startIndex, nameWord ->
        if (!nameWord.value.equals(leagueWords.first(), ignoreCase = true)) {
            return@forEachIndexed
        }
        var matchedWords = 0
        while (
            startIndex + matchedWords < nameWords.size &&
            matchedWords < leagueWords.size &&
            nameWords[startIndex + matchedWords].value.equals(
                leagueWords[matchedWords],
                ignoreCase = true,
            )
        ) {
            matchedWords += 1
        }
        if (
            matchedWords >= MinimumEmbeddedLeagueWords &&
            startIndex + matchedWords < nameWords.size
        ) {
            val suffixStart = nameWords[startIndex + matchedWords].range.first
            return trimmedName.substring(suffixStart).trim()
        }
    }
    return trimmedName
}

private fun matchingPrefixEnd(name: String, prefix: String): Int? {
    var nameIndex = 0
    var competitionIndex = 0
    while (competitionIndex < prefix.length) {
        if (nameIndex >= name.length) return null
        val competitionCharacter = prefix[competitionIndex]
        if (competitionCharacter.isWhitespace()) {
            if (!name[nameIndex].isWhitespace()) return null
            while (
                competitionIndex < prefix.length &&
                prefix[competitionIndex].isWhitespace()
            ) {
                competitionIndex += 1
            }
            while (nameIndex < name.length && name[nameIndex].isWhitespace()) {
                nameIndex += 1
            }
        } else {
            if (!competitionCharacter.equals(name[nameIndex], ignoreCase = true)) {
                return null
            }
            competitionIndex += 1
            nameIndex += 1
        }
    }

    if (nameIndex >= name.length || !name[nameIndex].isTeamNameSeparator()) {
        return null
    }
    while (nameIndex < name.length && name[nameIndex].isTeamNameSeparator()) {
        nameIndex += 1
    }
    return nameIndex.takeIf { it < name.length }
}

private fun Char.isTeamNameSeparator(): Boolean =
    isWhitespace() || this == '-' || this == ':' || this == '–' ||
        this == '—' || this == '|' || this == '/'

private val nonWhitespace = Regex("""\S+""")
private const val MinimumEmbeddedLeagueWords = 2
