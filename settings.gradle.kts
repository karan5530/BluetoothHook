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

        // JitPack repository for Sardine Android WebDAV library
        maven { url = uri("https://jitpack.io") }

        // Appodeal repository for Xposed Framework API
        maven { url = uri("https://artifactory.appodeal.com/appodeal-public/") }
    }
}

rootProject.name = "bluetoothhook"
include(":app")
 