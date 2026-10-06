package com.github.apkelly.drool.domain.usecase

import com.github.apkelly.drool.domain.repository.SportsRepository
import com.github.apkelly.drool.domain.repository.MatchWeatherRepository
import com.github.apkelly.drool.domain.model.Fixture
import com.github.apkelly.drool.domain.model.FamilyProfile

class ObserveClubsUseCase(private val repository: SportsRepository) {
    operator fun invoke() = repository.observeClubs()
}

class ObserveTeamsUseCase(private val repository: SportsRepository) {
    operator fun invoke(clubId: String? = null) = repository.observeTeams(clubId)
}

class ObserveFixturesUseCase(private val repository: SportsRepository) {
    operator fun invoke() = repository.observeFixtures()
    operator fun invoke(profileIds: Set<String>) = repository.observeFamilyFixtures(profileIds)
}

class ObserveFamilyTeamsUseCase(private val repository: SportsRepository) {
    operator fun invoke(profileIds: Set<String>) = repository.observeFamilyTeams(profileIds)
}

class ObserveFamilyClubsUseCase(private val repository: SportsRepository) {
    operator fun invoke(profileIds: Set<String>) = repository.observeFamilyClubs(profileIds)
}

class ObserveTeamRelationshipsUseCase(private val repository: SportsRepository) {
    operator fun invoke() = repository.observeTeamRelationships()
}

class RefreshClubsUseCase(private val repository: SportsRepository) {
    suspend operator fun invoke(force: Boolean = true) = repository.refreshClubs(force)
}

class RefreshTeamsUseCase(private val repository: SportsRepository) {
    suspend operator fun invoke(force: Boolean = true, clubId: String? = null) =
        repository.refreshTeams(force, clubId)
}

class RefreshFixturesUseCase(private val repository: SportsRepository) {
    suspend operator fun invoke(force: Boolean = true) = repository.refreshFixtures(force)
}

class RefreshFamilyProfilesUseCase(private val repository: SportsRepository) {
    suspend operator fun invoke(profiles: List<FamilyProfile>, force: Boolean = true) =
        repository.refreshFamilyProfiles(profiles, force)
}

class SetTeamFollowingUseCase(private val repository: SportsRepository) {
    suspend operator fun invoke(teamId: String, following: Boolean) =
        repository.setFollowing(teamId, following)
}

class LoadTeamHubUseCase(private val repository: SportsRepository) {
    suspend operator fun invoke(profileId: String, teamId: String, force: Boolean = false) =
        repository.loadTeamHub(profileId, teamId, force)
}

class LoadMatchDetailsUseCase(private val repository: SportsRepository) {
    suspend operator fun invoke(profileId: String, matchId: String) =
        repository.loadMatchDetails(profileId, matchId)
}

class LoadMatchWeatherUseCase(private val repository: MatchWeatherRepository) {
    suspend operator fun invoke(fixture: Fixture) = repository.loadMatchWeather(fixture)
}
