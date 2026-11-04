# App-specific R8 (code shrinker) rules for the release build.
#
# Most libraries used here (Room, Retrofit 3, kotlinx.serialization, WorkManager,
# Navigation) ship their own rules inside their artifacts, so nothing is needed
# here unless a release-only problem shows up. Keep this file small and explain
# every rule added.
