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
    id("org.jetbrains.kotlinx.atomicfu") version "0.29.0"
    id("io.github.ttypic.swiftklib") version "0.6.4"
    id("com.facebook.react")
    id("com.google.devtools.ksp") version "2.3.4"
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
                    create("ReactNativeBridge")
                    create("nskeyvalueobserving")
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

            // React Native
            implementation(libs.react.android)
            implementation(libs.hermes.android)
            implementation(libs.soloader)

            implementation(project   (":react-native-get-random-values"))
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
//            implementation(libs.ktor.server.sse)

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

            // network connectivity
            implementation(libs.connectivity.core)
            implementation(libs.connectivity.device)
            implementation(libs.connectivity.compose.device)

            implementation(libs.markdown.renderer.m3)

            // reakt-native-toolkit
            implementation(libs.reakt.native.toolkit)
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

react {
    root = rootProject.file("react-native")
    reactNativeDir = rootProject.file("react-native/node_modules/react-native")
    codegenDir = rootProject.file("react-native/node_modules/@react-native/codegen")
    cliFile = rootProject.file("react-native/node_modules/react-native/cli.js")
}

val bundleAndroidReleaseJs = tasks.register<Exec>("bundleAndroidReleaseJs") {
    group = "react"
    description = "Bundle React Native JavaScript for Android Release"

    workingDir = rootProject.file("react-native")

    val bundleFile = project.file("src/androidMain/assets/index.android.bundle")
    val assetsDir = project.file("src/androidMain/res")

    doFirst {
        bundleFile.parentFile.mkdirs()
        println("📦 Bundling React Native JavaScript for Android...")
    }

    commandLine(
        "npx", "react-native", "bundle",
        "--platform", "android",
        "--dev", "false",
        "--entry-file", "index.js",
        "--bundle-output", bundleFile.absolutePath,
        "--assets-dest", assetsDir.absolutePath,
        "--reset-cache"
    )

    doLast {
        println("✅ Android bundle created: ${bundleFile.absolutePath}")
    }
}

val bundleIOSReleaseJs = tasks.register<Exec>("bundleIOSReleaseJs") {
    group = "react"
    description = "Bundle React Native JavaScript for iOS Release"

    workingDir = rootProject.file("react-native")

    val bundleFile = rootProject.file("iosApp/iosApp/main.jsbundle")
    val assetsDir = rootProject.file("iosApp/iosApp")

    doFirst {
        bundleFile.parentFile.mkdirs()
        println("📦 Bundling React Native JavaScript for iOS...")
    }

    commandLine(
        "npx", "react-native", "bundle",
        "--platform", "ios",
        "--dev", "false",
        "--entry-file", "index.js",
        "--bundle-output", bundleFile.absolutePath,
        "--assets-dest", assetsDir.absolutePath
    )
}

dependencies {
    debugImplementation(compose.uiTooling)

    add("kspCommonMainMetadata", "de.voize:reakt-native-toolkit-ksp:0.22.1-SNAPSHOT")
    add("kspAndroid", "de.voize:reakt-native-toolkit-ksp:0.22.1-SNAPSHOT")
    add("kspIosArm64", "de.voize:reakt-native-toolkit-ksp:0.22.1-SNAPSHOT")
    add("kspIosSimulatorArm64", "de.voize:reakt-native-toolkit-ksp:0.22.1-SNAPSHOT")
}

ksp {
    arg("reakt.native.toolkit.kmpFrameworkName", "ComposeApp")
}

tasks.register<Copy>("copyGeneratedTsFiles") {
    from("build/generated/ksp/metadata/commonMain/resources/reaktNativeToolkit/typescript")
    into(rootProject.file("react-native/src/generated/reaktNativeToolkit/typescript"))
}

tasks.configureEach {
    if (name.startsWith("ksp") && name.contains("Kotlin")) {
        finalizedBy("copyGeneratedTsFiles")
    }
}

kotlin.sourceSets.commonMain {
    kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
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
        } else if (buildFlavor == "dev") {
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
        buildConfigField(
            STRING,
            "REVENUE_CAT_API_KEY",
            dotenv.get("REVENUE_CAT_API_KEY")!!
        )
    }
}

val syncXcodeVersionConfig = tasks.register<Exec>("syncXcodeVersionConfig") {
    workingDir(rootProject.projectDir)

    val iOSFirebaseFile = if (buildFlavor == "release") {
        "GoogleService-Info.release.plist"
    } else {
        "GoogleService-Info.dev.plist"
    }

    val envFile = if (buildFlavor == "release") {
        ".env.release"
    } else {
        ".env"
    }

    val script = """
        GID_CLIENT_ID=${'$'}(/usr/libexec/PlistBuddy -c "Print :CLIENT_ID" "firebase/$iOSFirebaseFile")
        GID_REVERSED_CLIENT_ID=${'$'}(/usr/libexec/PlistBuddy -c "Print :REVERSED_CLIENT_ID" "firebase/$iOSFirebaseFile")
        GID_SERVER_CLIENT_ID=${'$'}(grep GOOGLE_AUTH_SERVER_ID "$envFile" | cut -d'=' -f2 | tr -d ' "')

        cat > iosApp/Versions.xcconfig <<EOF
BUNDLE_SHORT_VERSION_STRING = $appVersionName
BUNDLE_VERSION = $appVersionCode
GID_CLIENT_ID = ${'$'}GID_CLIENT_ID
GID_SERVER_CLIENT_ID = ${'$'}GID_SERVER_CLIENT_ID
GID_REVERSED_CLIENT_ID = ${'$'}GID_REVERSED_CLIENT_ID
EOF
    """.trimIndent()

    commandLine("sh", "-c", script)
}

val syncFirebaseAndroid = tasks.register<Exec>("syncFirebaseAndroid") {
    group = "setup"
    description = "Copy Android Firebase config from ./firebase directory"

    workingDir(rootProject.projectDir)

    val androidFile = if (buildFlavor == "release") {
        "google-services.release.json"
    } else {
        "google-services.dev.json"
    }

    commandLine("cp", "firebase/$androidFile", "composeApp/google-services.json")

    doFirst {
        println("📱 Copying Android Firebase: $androidFile -> google-services.json")
    }
}

val syncFirebaseIOS = tasks.register<Exec>("syncFirebaseIOS") {
    group = "setup"
    description = "Copy iOS Firebase config from ./firebase directory"

    workingDir(rootProject.projectDir)

    val iOSFile = if (buildFlavor == "release") {
        "GoogleService-Info.release.plist"
    } else {
        "GoogleService-Info.dev.plist"
    }

    commandLine("cp", "firebase/$iOSFile", "iosApp/iosApp/GoogleService-Info.plist")

    doFirst {
        println("🍎 Copying iOS Firebase: $iOSFile -> iosApp/iosApp/GoogleService-Info.plist")
    }
}

tasks.named("preBuild").configure {
    dependsOn(syncFirebaseAndroid)
}

tasks.matching { it.name.contains("embedAndSign") && it.name.contains("FrameworkForXcode") }.configureEach {
    dependsOn(syncFirebaseIOS)
    dependsOn(syncXcodeVersionConfig)
}

swiftklib {
    create("StoreKitWrapper") {
        path = file("src/nativeInterop/storekit")
        packageName("app.slax.reader.storekit")
        minIos = 14
    }
    create("ReactNativeBridge") {
        path = file("src/nativeInterop/reactnative")
        packageName("app.slax.reader.reactnative.bridge")
        minIos = 14
    }
}