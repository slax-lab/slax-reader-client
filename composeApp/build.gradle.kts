import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import io.github.cdimascio.dotenv.dotenv
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import java.util.*
import io.github.ttypic.swiftklib.*
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

buildscript {
    repositories {
        mavenLocal()
        google()
        mavenCentral()
        gradlePluginPortal()
        maven {
            url = uri("https://plugins.gradle.org/m2/")
        }
    }
    dependencies {
        classpath(libs.kotlin.gradle.plugin)
        classpath(libs.buildkonfig.gradle.plugin)
        classpath(libs.dotenv.kotlin)
        classpath(libs.ttypic.plugin)
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
    id("org.jetbrains.kotlinx.atomicfu") version "0.31.0"
    id("io.github.ttypic.swiftklib") version "0.6.4"
}

repositories {
    mavenLocal()
    mavenCentral()
    google()
    maven("https://jogamp.org/deployment/maven")
}

val appVersionCode = project.findProperty("appVersionCode")?.toString()!!
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
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.compilations {
            val main by getting {
                cinterops {
                    create("StoreKitWrapper")
                    create("nskeyvalueobserving")
                    create("firebaseBridge")
                    create("googleSignInBridge")
                    create("RNBridge")
                }
            }
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.ui.tooling.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.koin.android)
            implementation(libs.androidx.browser)
            implementation(libs.sketch.animated.gif.koral)

            // firebase
            implementation(libs.firebase.analytics.ktx)
            implementation(libs.firebase.crashlytics.ktx)

            // Google Sign-In (Credential Manager)
            implementation(libs.android.credentials)
            implementation(libs.android.credentials.play.services.auth)
            implementation(libs.googleid)

            implementation(libs.reactnativeapp.get().toString()) {
                // Exclude old hermes group - RN 0.81 publishes hermes under com.facebook.react
                exclude(group = "com.facebook.hermes", module = "hermes-android")
            }
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        commonMain.dependencies {
            implementation(libs.jetbrains.runtime)
            implementation(libs.foundation)
            implementation(libs.material3)
            implementation(libs.ui)
            implementation(libs.components.resources)
            implementation(libs.org.jetbrains.compose.ui.ui.tooling.preview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            // navigation
            implementation(libs.navigation.compose)

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
            implementation(libs.sketch.compose)
            implementation(libs.sketch.http)
            implementation(libs.sketch.animated.gif)
            implementation(libs.sketch.svg)
            implementation(libs.sketch.compose.resources)
            implementation(libs.sketch.extensions.compose.resources)

            // IO/File
            implementation(libs.okio)

            // AtomicFU - required for Android runtime
            implementation(libs.atomicfu)

            // network connectivity
            implementation(libs.connectivity.core)
            implementation(libs.connectivity.device)
            implementation(libs.connectivity.compose.device)

            implementation(libs.markdown.renderer.m3)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        named { it.lowercase().startsWith("ios") }.configureEach {
            languageSettings {
                optIn("kotlinx.cinterop.ExperimentalForeignApi")
            }
        }
    }
}

// Provide version for com.facebook.react:hermes-android (worklets declares it without version)
configurations.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "com.facebook.react" && requested.name == "hermes-android") {
            useVersion("0.81.5")
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

fun minifyHtml(html: String): String {
    return html
}

@OptIn(ExperimentalEncodingApi::class)
fun base64Image(filePath: String) : String {
    return Base64.encode(file(filePath).readBytes())
}

buildkonfig {
    packageName = "app.slax.reader"
    objectName = "SlaxConfig"

    val dotenv = dotenv {
        directory = rootProject.projectDir.absolutePath
        filename = if (buildFlavor == "release") {
            ".env.release"
        } else {
            ".env"
        }
    }

    defaultConfigs {
        buildConfigField(STRING, "APP_NAME", "Slax Reader")
        buildConfigField(STRING, "APP_VERSION_NAME", appVersionName)
        buildConfigField(STRING, "APP_VERSION_CODE", appVersionCode)
        buildConfigField(STRING, "APP_BUNDLE_ID", "com.slax.reader")
        buildConfigField(STRING, "BUILD_ENV", buildFlavor)

        if (buildFlavor == "release") {
            buildConfigField(STRING, "API_BASE_URL", "https://api-reader.slax.com")
            buildConfigField(STRING, "WEB_BASE_URL", "https://r.slax.com")
            buildConfigField(STRING, "WEB_DOMAIN", ".slax.com")
            buildConfigField(STRING, "LOG_LEVEL", "ERROR")
        } else {
            buildConfigField(STRING, "API_BASE_URL", "https://reader-api.slax.dev")
            buildConfigField(STRING, "WEB_BASE_URL", "https://r.slax.dev")
            buildConfigField(STRING, "WEB_DOMAIN", ".slax.dev")
            buildConfigField(STRING, "LOG_LEVEL", "DEBUG")
        }

        buildConfigField(
            STRING,
            "WEBVIEW_TEMPLATE",
            minifyHtml(
                file("../public/embedded/html/webview-template.html")
                    .readText()
                    .replace("{{RESET-CSS}}", file("../public/embedded/css/reset.css").readText())
                    .replace("{{ARTICLE-CSS}}", file("../public/embedded/css/article.css").readText())
                    .replace("{{BOTTOM-LINE-CSS}}", file("../public/embedded/css/bottom-line.css").readText())
                    .replace("{{WEBVIEW-BRIGDE-JS}}", file("../public/embedded/js/webview-bridge.js").readText())
            )
        )
        buildConfigField(
            STRING,
            "DETAIL_ERROR_TEMPLATE",
            minifyHtml(
                file("../public/embedded/html/error.html")
                    .readText()
                    .replace("{{BACKGROUND}}", base64Image("../public/embedded/image/error.png"))
            )
        )
        buildConfigField(
            STRING,
            "GOOGLE_AUTH_SERVER_ID",
            dotenv.get("GOOGLE_AUTH_SERVER_ID")!!
        )
    }
}

val syncXcodeVersionConfig = tasks.register<Exec>("syncXcodeVersionConfig") {
    workingDir(rootProject.projectDir)
    val script = """
        cat > iosApp/Versions.xcconfig <<EOF
BUNDLE_SHORT_VERSION_STRING = $appVersionName
BUNDLE_VERSION = $appVersionCode
EOF
    """.trimIndent()

    commandLine("sh", "-c", script)
}

tasks.matching { it.name.contains("embedAndSign") && it.name.contains("FrameworkForXcode") }.configureEach {
    dependsOn(syncXcodeVersionConfig)
}

swiftklib {
    create("StoreKitWrapper") {
        path = file("src/nativeInterop/storekit")
        packageName("app.slax.reader.storekit")
        minIos = 14
    }
    create("RNBridge") {
        path = file("src/nativeInterop/rnbridge")
        packageName("app.slax.reader.rnbridge")
        minIos = 14
    }
}