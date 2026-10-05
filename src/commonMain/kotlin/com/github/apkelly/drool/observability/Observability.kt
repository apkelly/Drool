package com.github.apkelly.drool.observability

enum class AppScreen(internal val eventValue: String) {
    Welcome("welcome"),
    SignIn("sign_in"),
    Register("register"),
    Home("home"),
    Schedule("schedule"),
    Discover("discover"),
    Profile("profile"),
    Club("club"),
    Team("team"),
    MatchDetails("match_details"),
    PersonalInformation("personal_information"),
    LinkMember("link_member"),
    ApiDiagnostics("api_diagnostics"),
}

enum class SignInOutcome(internal val eventValue: String) {
    Success("success"),
    MissingFields("missing_fields"),
    InvalidCredentials("invalid_credentials"),
    Offline("offline"),
    ReconnectRequired("reconnect_required"),
    UnknownFailure("unknown_failure"),
}

enum class RefreshArea(internal val eventValue: String) {
    Clubs("clubs"),
    Teams("teams"),
    Fixtures("fixtures"),
}

enum class RefreshOutcome(internal val eventValue: String) {
    Updated("updated"),
    NotModified("not_modified"),
    Failed("failed"),
}

enum class NonFatalOperation(internal val eventValue: String) {
    RestoreSession("restore_session"),
    SignIn("sign_in"),
}

enum class ThemeSelection(internal val eventValue: String) {
    System("system"),
    Light("light"),
    Dark("dark"),
}

sealed interface AnalyticsEvent {
    data class ScreenViewed(val screen: AppScreen) : AnalyticsEvent
    data class SignInCompleted(val outcome: SignInOutcome) : AnalyticsEvent
    data class RefreshCompleted(
        val area: RefreshArea,
        val outcome: RefreshOutcome,
    ) : AnalyticsEvent

    data class ThemeSelected(val mode: ThemeSelection) : AnalyticsEvent
    data object SignedOut : AnalyticsEvent
}

interface Observability {
    fun setCollectionEnabled(enabled: Boolean)
    fun log(event: AnalyticsEvent)
    fun recordNonFatal(error: Throwable, operation: NonFatalOperation)
}

object NoOpObservability : Observability {
    override fun setCollectionEnabled(enabled: Boolean) = Unit
    override fun log(event: AnalyticsEvent) = Unit
    override fun recordNonFatal(error: Throwable, operation: NonFatalOperation) = Unit
}

internal data class AnalyticsPayload(
    val name: String,
    val parameterName: String? = null,
    val parameterValue: String? = null,
)

internal fun AnalyticsEvent.toPayload(): AnalyticsPayload =
    when (this) {
        is AnalyticsEvent.ScreenViewed -> AnalyticsPayload(
            name = "screen_view",
            parameterName = "screen_name",
            parameterValue = screen.eventValue,
        )
        is AnalyticsEvent.SignInCompleted -> AnalyticsPayload(
            name = "login_result",
            parameterName = "outcome",
            parameterValue = outcome.eventValue,
        )
        is AnalyticsEvent.RefreshCompleted -> AnalyticsPayload(
            name = "content_refresh",
            parameterName = "${area.eventValue}_result",
            parameterValue = outcome.eventValue,
        )
        is AnalyticsEvent.ThemeSelected -> AnalyticsPayload(
            name = "theme_selected",
            parameterName = "theme",
            parameterValue = mode.eventValue,
        )
        AnalyticsEvent.SignedOut -> AnalyticsPayload(name = "logout")
    }

expect fun createPlatformObservability(): Observability
