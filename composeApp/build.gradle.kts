@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import io.github.cdimascio.dotenv.dotenv
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import java.util.*

buildscript {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.0")
        classpath("com.codingfeline.buildkonfig:buildkonfig-gradle-plugin:0.17.1")
        classpath("io.github.cdimascio:dotenv-kotlin:6.5.1")
    }
}

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    kotlin("native.cocoapods")
    id("com.codingfeline.buildkonfig") version "0.17.1"
    id("org.jetbrains.kotlinx.atomicfu") version "0.29.0"
}

repositories {
    mavenCentral()
    google()
    maven("https://jogamp.org/deployment/maven")
}

fun incrementVersionCode(): String {
    val propsFile = File(rootProject.projectDir, "gradle.properties")
    val props = Properties()
    props.load(propsFile.inputStream())

    val currentVersionCode = props.getProperty("appVersionCode")?.toInt()!!
    val newVersionCode = currentVersionCode + 1

    props.setProperty("appVersionCode", newVersionCode.toString())
    props.store(propsFile.outputStream(), null)

    return newVersionCode.toString()
}

// Only increment version code when building (assemble or build task), otherwise use existing version code
val appVersionCode =
    if (gradle.startParameter.taskNames.any { it.contains("assemble") || it.contains("build") }) {
        incrementVersionCode()
    } else {
        project.findProperty("appVersionCode")?.toString()!!
    }

val appVersionName = project.findProperty("appVersionName")?.toString()!!
val buildFlavor = project.findProperty("buildkonfig.flavor") as? String ?: "dev"

kotlin {
    cocoapods {
        name = "ComposeApp"
        version = appVersionName
        summary = "Slax Reader Client"
        homepage = "https://github.com/slax-lab/slax-reader-client"
        ios.deploymentTarget = "14.1"

        podfile = project.file("../iosApp/Podfile")
        xcodeConfigurationToNativeBuildType["Release"] = NativeBuildType.RELEASE
        xcodeConfigurationToNativeBuildType["Debug"] = NativeBuildType.DEBUG

        pod("powersync-sqlite-core") {
            linkOnly = true
        }

        framework {
            baseName = "ComposeApp"
            isStatic = true
            export("com.powersync:core")
            binaryOption("bundleId", "com.slax.reader.composeapp")
        }
    }

    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

//    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.koin.android)
            implementation(libs.coil.network.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)

            // Coil network engine for iOS
            implementation(libs.coil.network.ktor)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            // navigation
            implementation(libs.navigation.compose)

            // log
            implementation(libs.napier)

            implementation(libs.datastore.preferences)

            // PowerSync
            api(libs.powerSyncCore)

            // DI
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.viewmodel)

            // HTTP client (for endpoint reachability checks)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)

            // Serialization
            implementation(libs.kotlinx.serialization.json)

            // time
            implementation(libs.kotlinx.datetime)

            // image
            implementation(libs.coil.compose)

            // firebase
            implementation(libs.firebase.app)
            implementation(libs.firebase.analytics)
            implementation(libs.firebase.crashlytics)

            // auth
            implementation(libs.kmpauth.google)

            // IO/File
            implementation(libs.okio)

            // AtomicFU - required for Android runtime
            implementation(libs.atomicfu)

            // browser tabs
            implementation(libs.androidx.browser)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "com.slax.reader"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.slax.reader"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = appVersionCode.toInt()
        versionName = appVersionName
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    signingConfigs {
        register("release") {
            storeFile = file("./slax-reader.release.jks")
            storePassword = System.getenv("SLAX_KEYSTORE_PASSWORD")
            keyAlias = "upload"
            keyPassword = System.getenv("SLAX_KEYSTORE_PASSWORD")
        }
    }
    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

buildkonfig {
    packageName = "app.slax.reader"
    objectName = "SlaxConfig"

    val dotenv = dotenv {
        directory = rootProject.projectDir.absolutePath
    }

    defaultConfigs {
        buildConfigField(STRING, "APP_NAME", "Slax Reader")
        buildConfigField(STRING, "APP_VERSION_NAME", appVersionName)
        buildConfigField(STRING, "APP_VERSION_CODE", appVersionCode)
    }

    defaultConfigs("dev") {
        buildConfigField(STRING, "BUILD_ENV", buildFlavor)
        buildConfigField(STRING, "API_BASE_URL", "https://reader-api.slax.dev")
        buildConfigField(STRING, "LOG_LEVEL", "DEBUG")
        buildConfigField(
            STRING,
            "GOOGLE_AUTH_SERVER_ID",
            dotenv.get("GOOGLE_AUTH_SERVER_ID")!!
        )
    }

    defaultConfigs("release") {
        buildConfigField(STRING, "BUILD_ENV", buildFlavor)
        buildConfigField(STRING, "API_BASE_URL", "https://api-reader.slax.com")
        buildConfigField(STRING, "LOG_LEVEL", "ERROR")
        buildConfigField(
            STRING,
            "GOOGLE_AUTH_SERVER_ID",
            dotenv.get("GOOGLE_AUTH_SERVER_ID")!!
        )
    }
}

tasks.register<Exec>("syncXcodeVersionConfig") {
    workingDir(layout.projectDirectory.dir("../iosApp"))

    val xconfigContent = """
        BUNDLE_SHORT_VERSION_STRING = $appVersionName
        BUNDLE_VERSION = $appVersionCode
    """.trimIndent()

    commandLine("sh", "-c", "echo '$xconfigContent' > Versions.xcconfig")
}
