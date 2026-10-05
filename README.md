# Drool

Drool is a Kotlin Multiplatform football client for the recovered Dribl API. It uses shared Compose UI on Android, iOS, and desktop, with an offline-first Room cache and a strict clean-architecture boundary.

## Targets

- Android, using Ktor's OkHttp engine
- Desktop JVM, using Ktor's OkHttp engine
- iOS device and Apple Silicon simulator (`iosArm64`, `iosSimulatorArm64`), using Ktor's Darwin engine

The original app's `okhttp/4.12.0` user-agent is sent for API compatibility. Kermit is the app-wide logging system for network, session, authentication, and refresh diagnostics. Authentication and normal API traffic use separate log tags. Authentication logging is headers-only, sensitive headers are redacted, and token/password fields are removed from any logged JSON.

## Architecture

- `com.github.apkelly.drool.data`: Ktor APIs, DTOs, mappers, Room entities/DAOs, DataStore preferences, and repository implementations.
- `com.github.apkelly.drool.domain`: business models, repository contracts, and use cases.
- `com.github.apkelly.drool.ui`: state holders, UI models, Material 3 screens, and reusable widgets.
- `com.github.apkelly.drool.di`: Koin modules and the composition graph.

The UI depends only on domain use cases. It never imports or calls the data layer directly.

The shared application uses:

- Material 3 with light, dark, and system themes
- Navigation 3 with typed, serializable routes and independent top-level back stacks
- Room as the local source of truth
- DataStore for the bearer token, active account, theme, and diagnostics consent
- Compottie for the shared splash animation
- Compose Multiplatform resources as the single localization catalog
- Native Google Maps SDK views on Android and iOS, plus Static Maps previews on desktop

## Google Maps configuration

Android and iOS require separate billing-enabled API keys with the **Maps SDK
for Android** and **Maps SDK for iOS** enabled. Restrict the Android key to the
`com.github.apkelly.drool` package and signing certificate, and restrict the iOS key to the
`com.github.apkelly.drool` bundle identifier. Do not commit either key.

Android reads `GOOGLE_MAPS_API_KEY` from a Gradle property or environment
variable. For local development it can also be stored in the ignored root
`local.properties` file:

```bash
GOOGLE_MAPS_API_KEY=your_android_key
# in local.properties, or:
GOOGLE_MAPS_API_KEY=your_android_key ./gradlew :androidApp:assembleDebug
# or: ./gradlew -PGOOGLE_MAPS_API_KEY=your_android_key :androidApp:assembleDebug
```

For iOS, create the ignored `iosApp/Secrets.xcconfig` file:

```text
GOOGLE_MAPS_API_KEY = your_ios_key
```

The tracked `iosApp/Config.xcconfig` imports this file when present. The key
can also be supplied to command-line builds:

```bash
xcodebuild -project iosApp/Drool.xcodeproj -scheme Drool \
  GOOGLE_MAPS_API_KEY=your_ios_key
```

The key is substituted into the built app's `Info.plist`; it is not stored in
source.

Desktop uses the **Maps Static API** because Google does not provide a native
desktop Maps SDK. Store its key in ignored `local.properties` for Gradle runs:

```text
GOOGLE_MAPS_STATIC_API_KEY=your_desktop_key
```

Alternatively, set the `GOOGLE_MAPS_STATIC_API_KEY` environment variable when
launching a packaged app. The desktop key is read only at runtime and is not
embedded in source or native distributions. Static map previews remain
clickable and open Google Maps driving directions.

## Firebase observability

Android and iOS include Firebase Analytics and Crashlytics using the registered
app identifiers `com.github.apkelly.drool`. Their platform configuration files
are versioned at `androidApp/google-services.json` and
`iosApp/iosApp/GoogleService-Info.plist`; Firebase documents these files as
containing non-secret project and app identifiers. Download replacements from
the Firebase console if either app registration changes.

Collection is disabled by default on both platforms. A user can explicitly
enable or disable **Share anonymous diagnostics** from Profile. The preference
controls Analytics and Crashlytics together and persists locally. Analytics
uses only fixed event and parameter values for screen categories, sign-in
outcomes, refresh outcomes, theme changes, and sign-out. Names, email
addresses, account/profile/team/match IDs, tokens, free-form text, and API
request or response data are never sent. Desktop uses a no-op observability
adapter.

Android uses the Google Services and Crashlytics Gradle plugins. The iOS target
links `FirebaseAnalytics` and `FirebaseCrashlytics` through Swift Package
Manager and runs the Crashlytics symbol-upload build phase. Release archives
therefore require network access to upload dSYMs.

## Offline behavior

Club and team catalogs are cached for 24 hours; fixtures are cached for 15 minutes. Home and Schedule provide a Family Hub selector with an **All profiles** overview and separate profile filters. Linked profiles are resolved through short-lived backend profile sessions, while fixtures and team relationships are stored with explicit profile ownership so overlapping fixture IDs cannot collide. Their personal details and emergency contacts are hydrated from the profile-scoped `/users/{userId}` and `/user-contacts/` endpoints and retained in the account-scoped cache. Cached content remains browsable when refresh fails, and list screens support pull-to-refresh. A stored root session with a cached profile, related users, and accounts can start offline. Signing out clears the token and all account-scoped profiles, linked users, personal details, accounts, fixtures, relationships, and cache metadata, so connectivity is required to authenticate again.

The current normal-user API surface is:

```text
POST /auth/signin
GET  /auth/related-users?email=...
GET  /access/accounts
GET  /linked-users
GET  /users/{userId}
GET  /user-contacts/
GET  /universal/clubs
GET  /universal/teams
GET  /universal/schedule
GET  /universal/matches
GET  /universal/matches/{matchId}
GET  /universal/ladders
GET  /universal/ladders/{ladderId}
```

The client accepts numeric Unix timestamps in seconds or milliseconds and ISO-8601 fixture timestamps. HTTP, response-format, transport, cancellation, and unexpected failures are classified separately. Admin-only endpoints are intentionally excluded.

The Profile screen includes an **API diagnostics** page for unresolved contracts: linked users, clubs, teams, and schedule. Each button runs only its named request so its full request/response can be inspected in the `OkHttp.Api` BODY logs. Confirmed related-user and access-account requests are no longer shown there. The original app also supplies tenant, season, competition, pagination, date-range, direction, and user context to the universal catalog/schedule endpoints; those recovered parameters are documented in `dribl-openapi.yaml`.

Authenticated diagnostics confirmed all five read contracts return HTTP 200. Unfiltered catalog calls are intentionally not treated as final user-scoped contracts: clubs returned 4,977 records, teams returned 1,000 records, and schedule returned cross-user `allocations`, `availabilities`, and `blockouts`. A future account-context milestone must derive the active tenant, season, competition, and Dribl user ID before these requests are narrowed for production behavior.

## Run and verify

Run the desktop app:

```bash
./gradlew run
```

Run desktop/common tests:

```bash
./gradlew desktopTest
```

Verify the mandatory 100% line and branch coverage gate for use cases and repository implementations:

```bash
./gradlew koverLogDesktop koverVerifyDesktop
```

Build desktop/common code:

```bash
./gradlew compileKotlinDesktop
```

Build Android when an Android SDK is installed:

```bash
./gradlew :androidApp:assembleDebug
```

Open `iosApp/Drool.xcodeproj` in Xcode to build and run the iOS host app. Its
build phase invokes Gradle's `embedAndSignAppleFrameworkForXcode` task and
embeds the shared Compose UI. To build only the iOS shared framework from the
command line:

```bash
./gradlew linkDebugFrameworkIosSimulatorArm64
```

Room schemas are exported to `schemas/` and must be versioned whenever the database changes. The shared root module uses the AGP 9 Android-KMP library plugin, while `androidApp` owns the Android application entry point. The build uses Gradle 9.6.0 and Android Gradle Plugin 9.4.1.

## Current scope

Drool currently includes session restoration, sign-in/sign-out, a summary-oriented Family Hub Home, a date-grouped full Schedule, attributed teams and clubs, club discovery, local account-scoped team following, and cached Profile lists refreshed from `GET /auth/related-users?email=...`, `GET /linked-users`, and `GET /access/accounts`. Team cards open branded hubs with Matches, Results, and Ladders; match rows open details where either team can be selected to continue browsing. Related-user identity fields, including date of birth, are reconciled into the canonical linked-user record so Profile, Home, Schedule, and Personal Information use the same family member. Profile provides a link-member FAB using the recovered `GET /linked-users-lookup` and `PATCH /linked-users/{id}` verification flow, while linked-user rows open Personal Information further hydrated from `GET /users/{userId}` and emergency contacts from `GET /user-contacts/`. Family profile sessions use `POST /auth/impersonate/{user_id}` internally without exposing backend switching terminology; team affiliations come from `GET /access/shortcut`, and club memberships come from `GET /universal/member-cards`. Association refresh failures preserve data from the authentication response or Room cache so the Profile remains available offline. Coil-backed profile/club/team/account imagery, responsive navigation, and dark mode are also included.

Firebase APIs remain behind platform observability adapters rather than leaking into feature, domain, or UI code.
