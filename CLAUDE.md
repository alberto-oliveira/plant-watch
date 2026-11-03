# Plant Care App
 
Personal-use Android app to remind me to water and fertilize my plants. Not for distribution: no Play Store, no accounts, no backend, no analytics, no ads. Sideloaded debug/release APK only.
 
**Guiding principle: keep it small.** Every feature must justify itself against "does this make the app bloated?". When in doubt, don't add it, and ask me first.
 
## Tech stack
 
- Kotlin, Jetpack Compose (Material 3), single-activity, single Gradle module (`:app`)
- Room for local storage (source of truth for everything)
- WorkManager for background scheduling (no per-plant alarms; no exact alarms)
- DataStore (Preferences) for settings
- Retrofit or Ktor + kotlinx.serialization for network (only for the optional features)
- Coil for images (only if photos are added)
- Navigation Compose, ViewModel + StateFlow, no Hilt (manual DI via a simple `AppContainer`)
- Use the Gradle version catalog (`gradle/libs.versions.toml`) for all dependencies
### SDK levels
- My phone runs Android 15 (API 35).
- `minSdk = 34`
- `targetSdk = 35` (matches my phone; bump when I change phones)
- `compileSdk = 37` (current AndroidX libraries require it; `platforms;android-37.0` is installed. Bumping further is fine if a library requires it: install the platform with `sdkmanager`)
- Set these in `app/build.gradle.kts`. If the generated template uses the block form for `compileSdk`, keep that style and just change the number.
- Lint's `NewApi` warnings matter: don't call APIs newer than API 35 without a version check.
### Toolchain
- JDK 21 is installed in WSL. Compile with JDK 21 but keep the Kotlin/Java target at 17 (`jvmTarget = "17"`) unless there's a reason to change it. Don't try to install or configure a different JDK toolchain.
- Gradle wrapper must be present in the repo (`gradlew`, `gradle/wrapper/`). If it's missing, generate it with `gradle wrapper` (an installed Gradle via SDKMAN may be used for that one step).
## Core behavior
 
### Plants and care tasks
- Register and deregister (delete) plants. Deleting a plant deletes its tasks and logs (cascade).
- Each plant has any number of care tasks: type (`WATER`, `FERTILIZE`, `OTHER` with a custom label), `intervalDays`, `lastDone`, `nextDue`.
- Marking a task done sets `lastDone = today` and `nextDue = today + intervalDays` (adjusted by the seasonal multiplier, if enabled) and writes a `CareLog` row.
### Reminders (design is fixed, don't change without asking)
- Store `nextDue` per task. Do **not** schedule one alarm per plant.
- One `PeriodicWorkRequest` runs daily at a configurable time (default 09:00). It finds tasks with `nextDue <= today` and posts **one grouped notification** (e.g. "Water: Monstera, Fern / Fertilize: Pothos").
- Notification actions: **Done** and **Snooze 1 day**, handled by a `BroadcastReceiver` that updates Room directly.
- Request `POST_NOTIFICATIONS` at runtime (Android 13+). Create a notification channel on app start.
- Exactness is not needed; approximate timing is fine.
- Aggressive battery management on some phones can delay background work. If reminders arrive late, check battery optimization settings for the app before changing the design.
## Optional features (build only after the core is solid)
 
### Sunlight tracking (Open-Meteo)
- Open-Meteo API: free, no API key. Use hourly `sunshine_duration`, `cloud_cover`, `direct_radiation`, `is_day`.
- Location is entered manually once (lat/lon in settings). **Do not use location permissions.**
- Each plant has a configured sun window (start/end hour) and an exposure factor (0-100%, e.g. 100 outdoors, ~40 by a window). The data measures sun at the location, not what the plant actually receives, so the factor matters.
- A daily worker fetches the data, sums sunshine within each plant's window, applies the factor, and stores one row per plant per day in Room. Weekly cumulative is a query, not stored data.
- Fetch at most once or twice per day; cache everything in Room.
### Plant database lookup
- Optional "suggest defaults when adding a plant" only. Never block adding a plant on network access.
- Candidates: Perenual (free key, small daily request limit, limited species coverage on free tier), Trefle, OpenPlantbook. Free-tier terms change, so verify before relying on any of them.
- Cache results in Room. Keep API keys in `local.properties` (git-ignored), exposed via `BuildConfig`. Never commit keys.
- A small hand-written preset list (succulent, tropical, herb, ...) is an acceptable alternative.
## Possible small extras (ask before adding)
- Seasonal interval multiplier (water less in winter)
- Skip watering reminders for outdoor plants after rain (precipitation from Open-Meteo)
- Frost alert for outdoor plants from forecast minimum temperature
- Per-plant notes and a photo
- Home-screen widget (Jetpack Glance) with "due today"
- JSON export/import for backup
## Code conventions
- Package by feature (`plants`, `reminders`, `weather`, `settings`), with `data`/`ui` inside each when it grows.
- Room entities and DAOs return `Flow` for observed data; suspend functions for writes.
- Use `java.time` (`LocalDate`, `ZoneId`) for all date logic. Store dates as ISO strings or epoch days via type converters. Never store "days remaining".
- Keep business logic (e.g. computing `nextDue`) in plain Kotlin functions that are unit-testable without Android.
- No premature abstraction: avoid repositories/use-case layers unless there is real logic in them.
- Compose: stateless composables with state hoisted to ViewModels. Provide `@Preview`s for the main screens.
- All user-facing strings in `strings.xml`.
- Room: export schema, write migrations from the first schema change (don't use destructive migration once I have real data in the app).
## Testing
- Unit tests for: next-due calculation, snooze logic, sunlight-window summation, seasonal multiplier.
- Room DAO tests via instrumented tests only when the query is non-trivial.
- Don't aim for coverage numbers; test the logic that can silently go wrong.
## Development environment (WSL)
- Developing in WSL (Ubuntu). The repo lives on the WSL ext4 filesystem under `~/`, **not** under `/mnt/c`.
- Android SDK is at `~/Android/Sdk` (`ANDROID_HOME`), with `cmdline-tools/latest`, `platform-tools`, `platforms;android-35`, `platforms;android-37.0` and `build-tools;35.0.0` installed. `sdkmanager` and `adb` are on `PATH`.
- `local.properties` holds `sdk.dir=/home/alberto/Android/Sdk` (git-ignored; never commit it).
- `adb` is the WSL one. There should be no Windows `adb` in play; if `adb devices` misbehaves, check `which -a adb`.
- No emulator in WSL. I test on my physical phone (Android 15) over **wireless debugging** (`adb pair` / `adb connect`), or USB via `usbipd-win` as a fallback. Don't try to set up or start an emulator.
- If builds are slow or WSL runs out of memory, the fix is the memory limit in `.wslconfig` and `org.gradle.jvmargs` in `gradle.properties`, not changing the project structure.
## Build and run
- Build debug APK: `./gradlew assembleDebug`
- Install on the connected phone: `./gradlew installDebug` (check `adb devices` first)
- Unit tests: `./gradlew testDebugUnitTest`
- Lint: `./gradlew lint`
- Always run `./gradlew assembleDebug` and the unit tests before telling me a task is done.
- The first build is slow (dependency downloads); later builds are much faster.
## Working agreement
- Make small, focused commits with clear messages; work on a branch per feature.
- Before adding a new dependency, tell me what it's for and whether the platform already covers it.
- Prefer the simplest solution that works. If a requirement is ambiguous, ask instead of guessing.
- Don't add telemetry, crash reporting, ads, login, or cloud sync.
- Never commit secrets, `local.properties`, keystores, or APKs.
- Only lightweight, core tests; Don't expend too many tokens on tests
## Suggested build order
1. Project skeleton (including Gradle wrapper), Room schema, plant list + add/delete screens
2. Care tasks per plant (water first, then fertilize/other), mark-done flow
3. Daily worker + grouped notification + Done/Snooze actions
4. Settings (notification time, permissions flow)
5. Sunlight tracking
6. Plant database lookup / presets
7. Small extras, one at a time

# Ground-rules
Before every development stage, I will start with planning the next development step. Please consider the following ground-rules when planning:
- You can create git issues and git branches, but leave staging and commiting to me. If you need to perform other git writing operations, you can ask me; git reading is fine.
- Break-down development on  small, commitable steps, as the working agreement says. After every step, give me:
    - What are the new files, and their purpose
    - What changed, and why each file has been changed
    - What files have been removed, and why
    - Deviations from the plan
    - Git staging command
    - Suggested git message
- Try to be didactic with your commits. Although I know programming, I don't know Kotlin and intend to use your work to learn it
