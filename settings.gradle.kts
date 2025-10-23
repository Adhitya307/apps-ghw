pluginManagement {
    repositories {
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
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

// ✅ Nama root project
rootProject.name = "KERJA PRAKTIK"

// ✅ Daftarkan dua modul sejajar (bukan nested)
include(":app-rembesan")
project(":app-rembesan").projectDir = file("app-rembesan")

include(":app-DamBody")
project(":app-DamBody").projectDir = file("app-DamBody")
