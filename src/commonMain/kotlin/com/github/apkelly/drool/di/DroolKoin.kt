package com.github.apkelly.drool.di

import io.ktor.client.HttpClient
import com.github.apkelly.drool.data.local.createDroolDatabase
import com.github.apkelly.drool.data.remote.DriblApi
import com.github.apkelly.drool.data.remote.DriblSportsApi
import com.github.apkelly.drool.data.remote.AuthRemoteDataSource
import com.github.apkelly.drool.data.remote.SportsRemoteDataSource
import com.github.apkelly.drool.data.remote.createPlatformHttpClient
import com.github.apkelly.drool.data.repository.PreferencesRepositoryImpl
import com.github.apkelly.drool.data.repository.SessionRepositoryImpl
import com.github.apkelly.drool.data.repository.SportsRepositoryImpl
import com.github.apkelly.drool.data.storage.createPlatformBearerTokenStore
import com.github.apkelly.drool.data.time.SystemTimeProvider
import com.github.apkelly.drool.data.time.TimeProvider
import com.github.apkelly.drool.domain.repository.PreferencesRepository
import com.github.apkelly.drool.domain.repository.SessionRepository
import com.github.apkelly.drool.domain.repository.SportsRepository
import com.github.apkelly.drool.domain.usecase.ObserveClubsUseCase
import com.github.apkelly.drool.domain.usecase.ObserveFixturesUseCase
import com.github.apkelly.drool.domain.usecase.ObserveFamilyTeamsUseCase
import com.github.apkelly.drool.domain.usecase.ObserveFamilyClubsUseCase
import com.github.apkelly.drool.domain.usecase.ObserveTeamRelationshipsUseCase
import com.github.apkelly.drool.domain.usecase.ObserveTeamsUseCase
import com.github.apkelly.drool.domain.usecase.ObserveThemeModeUseCase
import com.github.apkelly.drool.domain.usecase.ObserveObservabilityEnabledUseCase
import com.github.apkelly.drool.domain.usecase.ObserveProfileUseCase
import com.github.apkelly.drool.domain.usecase.RefreshClubsUseCase
import com.github.apkelly.drool.domain.usecase.RefreshFixturesUseCase
import com.github.apkelly.drool.domain.usecase.RefreshFamilyProfilesUseCase
import com.github.apkelly.drool.domain.usecase.RefreshTeamsUseCase
import com.github.apkelly.drool.domain.usecase.RefreshProfileEndpointUseCase
import com.github.apkelly.drool.domain.usecase.GetLinkCandidatesUseCase
import com.github.apkelly.drool.domain.usecase.LoadTeamHubUseCase
import com.github.apkelly.drool.domain.usecase.LoadMatchDetailsUseCase
import com.github.apkelly.drool.domain.usecase.VerifyLinkedUserUseCase
import com.github.apkelly.drool.domain.usecase.RestoreSessionUseCase
import com.github.apkelly.drool.domain.usecase.SetTeamFollowingUseCase
import com.github.apkelly.drool.domain.usecase.SetThemeModeUseCase
import com.github.apkelly.drool.domain.usecase.SetObservabilityEnabledUseCase
import com.github.apkelly.drool.domain.usecase.SignInUserUseCase
import com.github.apkelly.drool.domain.usecase.SignOutUserUseCase
import com.github.apkelly.drool.ui.viewmodel.AppViewModel
import com.github.apkelly.drool.ui.viewmodel.SportsViewModel
import com.github.apkelly.drool.observability.Observability
import com.github.apkelly.drool.observability.createPlatformObservability
import org.koin.core.Koin
import org.koin.core.context.startKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.mp.KoinPlatform

private val authHttpClient = named("authHttpClient")
private val sportsHttpClient = named("sportsHttpClient")

private val dataModule = module {
    single<HttpClient>(authHttpClient) {
        createPlatformHttpClient(logBodies = false, logTag = "OkHttp.Auth")
    }
    single<HttpClient>(sportsHttpClient) {
        createPlatformHttpClient(logBodies = true, logTag = "OkHttp.Api")
    }
    single { createPlatformBearerTokenStore() }
    single { createDroolDatabase() }
    single<TimeProvider> { SystemTimeProvider }
    single<AuthRemoteDataSource> { DriblApi(client = get(authHttpClient)) }
    single<SportsRemoteDataSource> { DriblSportsApi(client = get(sportsHttpClient)) }
    single<SessionRepository> {
        SessionRepositoryImpl(
            authApi = get(),
            sportsApi = get(),
            store = get(),
            database = get(),
        )
    }
    single<SportsRepository> {
        SportsRepositoryImpl(
            api = get(),
            database = get(),
            store = get(),
            timeProvider = get(),
        )
    }
    single<PreferencesRepository> { PreferencesRepositoryImpl(store = get()) }
    single<Observability> { createPlatformObservability() }
}

private val domainModule = module {
    factory { ObserveProfileUseCase(repository = get()) }
    factory { RestoreSessionUseCase(repository = get()) }
    factory { SignInUserUseCase(repository = get()) }
    factory { SignOutUserUseCase(repository = get()) }
    factory { RefreshProfileEndpointUseCase(repository = get()) }
    factory { GetLinkCandidatesUseCase(repository = get()) }
    factory { VerifyLinkedUserUseCase(repository = get()) }
    factory { ObserveThemeModeUseCase(repository = get()) }
    factory { SetThemeModeUseCase(repository = get()) }
    factory { ObserveObservabilityEnabledUseCase(repository = get()) }
    factory { SetObservabilityEnabledUseCase(repository = get()) }
    factory { ObserveClubsUseCase(repository = get()) }
    factory { ObserveTeamsUseCase(repository = get()) }
    factory { ObserveFixturesUseCase(repository = get()) }
    factory { ObserveFamilyTeamsUseCase(repository = get()) }
    factory { ObserveFamilyClubsUseCase(repository = get()) }
    factory { ObserveTeamRelationshipsUseCase(repository = get()) }
    factory { RefreshClubsUseCase(repository = get()) }
    factory { RefreshTeamsUseCase(repository = get()) }
    factory { RefreshFixturesUseCase(repository = get()) }
    factory { RefreshFamilyProfilesUseCase(repository = get()) }
    factory { SetTeamFollowingUseCase(repository = get()) }
    factory { LoadTeamHubUseCase(repository = get()) }
    factory { LoadMatchDetailsUseCase(repository = get()) }
}

private val uiModule = module {
    single {
        AppViewModel(
            observeProfile = get(),
            restoreSession = get(),
            signInUser = get(),
            signOutUser = get(),
            refreshProfileEndpoint = get(),
            getLinkCandidates = get(),
            verifyLinkedUserUseCase = get(),
            observeThemeMode = get(),
            setThemeMode = get(),
            observeObservabilityEnabled = get(),
            setObservabilityEnabled = get(),
            observability = get(),
        )
    }
    single {
        SportsViewModel(
            observeClubs = get(),
            observeTeams = get(),
            observeFixtures = get(),
            observeFamilyTeams = get(),
            observeFamilyClubs = get(),
            observeRelationships = get(),
            refreshClubsUseCase = get(),
            refreshTeamsUseCase = get(),
            refreshFixturesUseCase = get(),
            refreshFamilyProfiles = get(),
            setTeamFollowing = get(),
            loadTeamHub = get(),
            loadMatchDetails = get(),
            observability = get(),
        )
    }
}

fun initDroolKoin(): Koin =
    KoinPlatform.getKoinOrNull() ?: startKoin {
        modules(dataModule, domainModule, uiModule)
    }.koin
