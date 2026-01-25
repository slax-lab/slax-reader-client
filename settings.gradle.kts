rootProject.name = "slax-reader-client"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        maven(url = "https://maven.mozilla.org/maven2")
        maven("https://jogamp.org/deployment/maven")
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        maven("https://maven.mozilla.org/maven2")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(":composeApp")
include(":react-native-get-random-values")
project(":react-native-get-random-values").projectDir = file("react-native/node_modules/react-native-get-random-values/android")

includeBuild("react-native/node_modules/@react-native/gradle-plugin")

includeBuild("../reakt-native-toolkit/kotlin") {
    dependencySubstitution {
        substitute(module("de.voize:reakt-native-toolkit")).using(project(":reakt-native-toolkit"))
        substitute(module("de.voize:reakt-native-toolkit-ksp")).using(project(":reakt-native-toolkit-ksp"))
    }
}