// Root build script. It only declares plugin versions (`apply false`) so that
// modules can apply them without repeating the version. No code lives here.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
