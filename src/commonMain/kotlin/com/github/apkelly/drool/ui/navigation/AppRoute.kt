package com.github.apkelly.drool.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface AppRoute : NavKey {
    @Serializable
    data object Welcome : AppRoute

    @Serializable
    data object SignIn : AppRoute

    @Serializable
    data object Register : AppRoute

    @Serializable
    data object Home : AppRoute

    @Serializable
    data object Schedule : AppRoute

    @Serializable
    data object Discover : AppRoute

    @Serializable
    data class Club(val clubId: String) : AppRoute

    @Serializable
    data class Team(
        val profileId: String,
        val teamId: String,
        val teamName: String,
        val logoUrl: String? = null,
        val primaryColor: String? = null,
        val secondaryColor: String? = null,
    ) : AppRoute

    @Serializable
    data class MatchDetails(
        val profileId: String,
        val matchId: String,
    ) : AppRoute

    @Serializable
    data object Profile : AppRoute

    @Serializable
    data class PersonalInformation(val userId: String) : AppRoute

    @Serializable
    data object LinkMember : AppRoute

    @Serializable
    data object ApiDiagnostics : AppRoute
}

enum class TopLevelDestination {
    Home,
    Schedule,
    Discover,
    Profile,
}
