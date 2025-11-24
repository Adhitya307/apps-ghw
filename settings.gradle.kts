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

// ✅ Daftarkan semua modul sejajar (bukan nested)
include(":app-rembesan")
project(":app-rembesan").projectDir = file("app-rembesan")

include(":app-DamBody")
project(":app-DamBody").projectDir = file("app-DamBody")

include(":app-BubbleTilt")
project(":app-BubbleTilt").projectDir = file("app-BubbleTilt")  // TAMBAH BARIS INI
include(":app-exstenso")
include(":app-LeftPiezo")
include(":app-RightPiezo")
