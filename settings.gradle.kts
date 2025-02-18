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
        maven { setUrl("https://jitpack.io") }
    }
}

rootProject.name = "DigitalTablet"
include(":app")
// ✅ 加入 DigitalRobot 作為 Module
include(":digitalrobot")
project(":digitalrobot").projectDir = file("/Users/sianglingzhang/AndroidStudioProjects/Digital-Robot-0211/app")
 