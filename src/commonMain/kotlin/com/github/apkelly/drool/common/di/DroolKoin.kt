package com.github.apkelly.drool.common.di

import com.github.apkelly.dribl.api.AuthRemoteDataSource
import com.github.apkelly.dribl.api.DriblApi
import com.github.apkelly.dribl.api.DriblSportsApi
import com.github.apkelly.dribl.api.DriblTokenRenewer
import com.github.apkelly.dribl.api.DriblTokenStore
import com.github.apkelly.dribl.api.SportsRemoteDataSource
import com.github.apkelly.dribl.api.withAutomaticDriblTokenRenewal
import com.github.apkelly.drool.app.ui.viewmodel.AppViewModel
import com.github.apkelly.drool.app.ui.viewmodel.SportsViewModel
import com.github.apkelly.drool.auth.data.AuthRepositoryImpl
import com.github.apkelly.drool.auth.domain.repository.AuthRepository
import com.github.apkelly.drool.auth.domain.usecase.ObserveProfileUseCase
import com.github.apkelly.drool.auth.domain.usecase.RestoreSessionUseCase
import com.github.apkelly.drool.auth.domain.usecase.SignInUserUseCase
import com.github.apkelly.drool.auth.domain.usecase.SignOutUserUseCase
import com.github.apkelly.drool.common.data.local.createDroolDatabase
import com.github.apkelly.drool.common.data.remote.createPlatformHttpClient
import com.github.apkelly.drool.common.data.repository.SessionDataSource
import com.github.apkelly.drool.common.data.repository.SessionDataSourceImpl
import com.github.apkelly.drool.common.data.repository.SportsDataSource
import com.github.apkelly.drool.common.data.repository.SportsDataSourceImpl
import com.github.apkelly.drool.common.data.storage.BearerTokenStore
import com.github.apkelly.drool.common.data.storage.createPlatformBearerTokenStore
import com.github.apkelly.drool.common.data.time.SystemTimeProvider
import com.github.apkelly.drool.common.data.time.TimeProvider
import com.github.apkelly.drool.common.observability.Observability
import com.github.apkelly.drool.common.observability.createPlatformObservability
import com.github.apkelly.drool.discover.data.DiscoverRepositoryImpl
import com.github.apkelly.drool.discover.domain.repository.DiscoverRepository
import com.github.apkelly.drool.discover.domain.usecase.ObserveClubsUseCase
import com.github.apkelly.drool.discover.domain.usecase.ObserveTeamRelationshipsUseCase
import com.github.apkelly.drool.discover.domain.usecase.ObserveTeamsUseCase
import com.github.apkelly.drool.discover.domain.usecase.RefreshClubsUseCase
import com.github.apkelly.drool.discover.domain.usecase.RefreshTeamsUseCase
import com.github.apkelly.drool.discover.domain.usecase.SetTeamFollowingUseCase
import com.github.apkelly.drool.home.data.HomeRepositoryImpl
import com.github.apkelly.drool.home.domain.repository.HomeRepository
import com.github.apkelly.drool.home.domain.usecase.ObserveProfileClubsUseCase
import com.github.apkelly.drool.home.domain.usecase.ObserveProfileTeamsUseCase
import com.github.apkelly.drool.match.data.MatchRepositoryImpl
import com.github.apkelly.drool.match.data.weather.GoogleWeatherApi
import com.github.apkelly.drool.match.data.weather.MatchWeatherDataSource
import com.github.apkelly.drool.match.data.weather.MatchWeatherDataSourceImpl
import com.github.apkelly.drool.match.data.weather.WeatherRemoteDataSource
import com.github.apkelly.drool.match.data.weather.platformWeatherApiKey
import com.github.apkelly.drool.match.data.weather.platformWeatherRequestHeaders
import com.github.apkelly.drool.match.domain.repository.MatchRepository
import com.github.apkelly.drool.match.domain.usecase.LoadMatchDetailsUseCase
import com.github.apkelly.drool.match.domain.usecase.LoadMatchWeatherUseCase
import com.github.apkelly.drool.profile.data.PreferencesRepositoryImpl
import com.github.apkelly.drool.profile.data.ProfileRepositoryImpl
import com.github.apkelly.drool.profile.domain.repository.PreferencesRepository
import com.github.apkelly.drool.profile.domain.repository.ProfileRepository
import com.github.apkelly.drool.profile.domain.usecase.GetLinkCandidatesUseCase
import com.github.apkelly.drool.profile.domain.usecase.ObserveObservabilityEnabledUseCase
import com.github.apkelly.drool.profile.domain.usecase.ObserveThemeModeUseCase
import com.github.apkelly.drool.profile.domain.usecase.RequestLinkedUserUseCase
import com.github.apkelly.drool.profile.domain.usecase.SetObservabilityEnabledUseCase
import com.github.apkelly.drool.profile.domain.usecase.SetThemeModeUseCase
import com.github.apkelly.drool.profile.domain.usecase.UnlinkLinkedUserUseCase
import com.github.apkelly.drool.profile.domain.usecase.VerifyLinkedUserUseCase
import com.github.apkelly.drool.schedule.data.AvailabilityRepositoryImpl
import com.github.apkelly.drool.schedule.data.ScheduleRepositoryImpl
import com.github.apkelly.drool.schedule.domain.repository.AvailabilityRepository
import com.github.apkelly.drool.schedule.domain.repository.ScheduleRepository
import com.github.apkelly.drool.schedule.domain.usecase.CreateRefereeAvailabilityUseCase
import com.github.apkelly.drool.schedule.domain.usecase.DeleteRefereeAvailabilityUseCase
import com.github.apkelly.drool.schedule.domain.usecase.GetRefereeAvailabilityUseCase
import com.github.apkelly.drool.schedule.domain.usecase.GetRefereeClubsUseCase
import com.github.apkelly.drool.schedule.domain.usecase.ObserveFixturesUseCase
import com.github.apkelly.drool.schedule.domain.usecase.RefreshFixturesUseCase
import com.github.apkelly.drool.schedule.domain.usecase.RefreshLinkedProfilesUseCase
import com.github.apkelly.drool.schedule.ui.viewmodel.AvailabilityViewModel
import com.github.apkelly.drool.team.data.TeamRepositoryImpl
import com.github.apkelly.drool.team.domain.repository.TeamRepository
import com.github.apkelly.drool.team.domain.usecase.LoadTeamHubUseCase
import io.ktor.client.HttpClient
import org.koin.core.Koin
import org.koin.core.context.startKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.mp.KoinPlatform

private val authHttpClient = named("authHttpClient")
private val sportsHttpClient = named("sportsHttpClient")
private val weatherHttpClient = named("weatherHttpClient")

private val dataModule = module {
    single<HttpClient>(authHttpClient) {
        createPlatformHttpClient(logBodies = false, logTag = "OkHttp.Auth")
    }
    single<HttpClient>(sportsHttpClient) {
        createPlatformHttpClient(logBodies = true, logTag = "OkHttp.Api")
            .withAutomaticDriblTokenRenewal(get())
    }
    single<HttpClient>(weatherHttpClient) {
        createPlatformHttpClient(
            logBodies = false,
            logTag = "GoogleWeather",
            loggingEnabled = false,
        )
    }
    single { createPlatformBearerTokenStore() }
    single<DriblTokenStore> { get<BearerTokenStore>() }
    single { DriblTokenRenewer(get(authHttpClient), get()) }
    single { createDroolDatabase() }
    single<TimeProvider> { SystemTimeProvider }
    single<AuthRemoteDataSource> { DriblApi(client = get(authHttpClient)) }
    single<SportsRemoteDataSource> { DriblSportsApi(client = get(sportsHttpClient)) }
    single<WeatherRemoteDataSource> {
        GoogleWeatherApi(
            client = get(weatherHttpClient),
            requestHeaders = platformWeatherRequestHeaders(),
        )
    }
    single<SessionDataSource> {
        SessionDataSourceImpl(
            authApi = get(),
            sportsApi = get(),
            store = get(),
            database = get(),
        )
    }
    single<SportsDataSource> {
        SportsDataSourceImpl(
            api = get(),
            database = get(),
            store = get(),
            timeProvider = get(),
        )
    }
    single<AuthRepository> { AuthRepositoryImpl(sessionDataSource = get()) }
    single<ProfileRepository> { ProfileRepositoryImpl(sessionDataSource = get()) }
    single<HomeRepository> { HomeRepositoryImpl(sportsDataSource = get()) }
    single<ScheduleRepository> { ScheduleRepositoryImpl(sportsDataSource = get()) }
    single<AvailabilityRepository> { AvailabilityRepositoryImpl(get(), get(), get()) }
    single<DiscoverRepository> { DiscoverRepositoryImpl(sportsDataSource = get()) }
    single<TeamRepository> { TeamRepositoryImpl(sportsDataSource = get()) }
    single<PreferencesRepository> { PreferencesRepositoryImpl(store = get()) }
    single<MatchWeatherDataSource> {
        MatchWeatherDataSourceImpl(
            api = get(),
            database = get(),
            timeProvider = get(),
            apiKey = platformWeatherApiKey(),
        )
    }
    single<MatchRepository> {
        MatchRepositoryImpl(
            sportsDataSource = get(),
            weatherDataSource = get(),
        )
    }
    single<Observability> { createPlatformObservability() }
}

private val domainModule = module {
    factory { ObserveProfileUseCase(repository = get()) }
    factory { RestoreSessionUseCase(repository = get()) }
    factory { SignInUserUseCase(repository = get()) }
    factory { SignOutUserUseCase(repository = get()) }
    factory { GetLinkCandidatesUseCase(repository = get()) }
    factory { RequestLinkedUserUseCase(repository = get()) }
    factory { VerifyLinkedUserUseCase(repository = get()) }
    factory { UnlinkLinkedUserUseCase(repository = get()) }
    factory { ObserveThemeModeUseCase(repository = get()) }
    factory { SetThemeModeUseCase(repository = get()) }
    factory { ObserveObservabilityEnabledUseCase(repository = get()) }
    factory { SetObservabilityEnabledUseCase(repository = get()) }
    factory { ObserveClubsUseCase(repository = get()) }
    factory { ObserveTeamsUseCase(repository = get()) }
    factory { ObserveFixturesUseCase(repository = get()) }
    factory { ObserveProfileTeamsUseCase(repository = get()) }
    factory { ObserveProfileClubsUseCase(repository = get()) }
    factory { ObserveTeamRelationshipsUseCase(repository = get()) }
    factory { RefreshClubsUseCase(repository = get()) }
    factory { RefreshTeamsUseCase(repository = get()) }
    factory { RefreshFixturesUseCase(repository = get()) }
    factory { RefreshLinkedProfilesUseCase(repository = get()) }
    factory { GetRefereeClubsUseCase(get()) }
    factory { GetRefereeAvailabilityUseCase(get()) }
    factory { CreateRefereeAvailabilityUseCase(get()) }
    factory { DeleteRefereeAvailabilityUseCase(get()) }
    factory { SetTeamFollowingUseCase(repository = get()) }
    factory { LoadTeamHubUseCase(repository = get()) }
    factory { LoadMatchDetailsUseCase(repository = get()) }
    factory { LoadMatchWeatherUseCase(repository = get()) }
}

private val uiModule = module {
    single {
        AppViewModel(
            observeProfile = get(),
            restoreSession = get(),
            signInUser = get(),
            signOutUser = get(),
            getLinkCandidates = get(),
            requestLinkedUserUseCase = get(),
            verifyLinkedUserUseCase = get(),
            unlinkLinkedUserUseCase = get(),
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
            observeProfileTeams = get(),
            observeProfileClubs = get(),
            observeRelationships = get(),
            refreshClubsUseCase = get(),
            refreshTeamsUseCase = get(),
            refreshFixturesUseCase = get(),
            refreshLinkedProfiles = get(),
            setTeamFollowing = get(),
            loadTeamHub = get(),
            loadMatchDetails = get(),
            loadMatchWeather = get(),
            observability = get(),
        )
    }
    single {
        AvailabilityViewModel(
            getRefereeClubs = get(),
            getAvailability = get(),
            createAvailability = get(),
            deleteAvailability = get(),
        )
    }
}

fun initDroolKoin(): Koin = KoinPlatform.getKoinOrNull() ?: startKoin {
    modules(dataModule, domainModule, uiModule)
}.koin
