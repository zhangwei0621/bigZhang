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
        maven {
            // Use github hosted maven repo for now.
            // Repo url: https://github.com/wysaid/android-gpuimage-plus-maven
            url = uri("https://maven.wysaid.org/")
        }
    }
}

rootProject.name = "bigZhang"
include(":app")
 