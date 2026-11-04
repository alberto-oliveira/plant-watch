// settings.gradle.kts — evaluated first. Declares where plugins/libraries come
// from and which modules belong to the build (here, only ":app").

pluginManagement {
    repositories {
        // google() hosts the Android Gradle Plugin and AndroidX; restrict it to those
        // groups so Gradle doesn't ask Google's repo for everything else.
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // Modules may not declare their own repositories: everything is defined here.
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "plant-watch"
include(":app")
