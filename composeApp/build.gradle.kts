import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import io.github.cdimascio.dotenv.dotenv
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import java.util.*
import io.github.ttypic.swiftklib.*

buildscript {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven {
            url = uri("https://plugins.gradle.org/m2/")
        }
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.0")
        classpath("com.codingfeline.buildkonfig:buildkonfig-gradle-plugin:0.17.1")
        classpath("io.github.cdimascio:dotenv-kotlin:6.5.1")
        classpath("io.github.ttypic:plugin:0.6.4")
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

    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.compilations {
            val main by getting {
                cinterops {
                    create("StoreKitWrapper")
                    create("nskeyvalueobserving")
                }
            }
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.koin.android)
            implementation(libs.androidx.browser)
            implementation(libs.sketch.animated.gif.koral)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

fun minifyHtml(html: String): String {
    return html
}

/**
 * 获取 web-bridge 产物文件路径
 */
fun getWebBridgeOutputPath(): File {
    val webBridgeDir = file("../public/embedded/sub-projects/web-bridge")
    return file("$webBridgeDir/dist/slax-reader-web-bridge.js")
}

/**
 * 读取 web-bridge 文件内容
 * 如果文件不存在，直接报错
 */
fun getWebBridgeContent(): String {
    val output = getWebBridgeOutputPath()
    return if (output.exists()) {
        output.readText()
    } else {
        throw GradleException("""
            ❌ web-bridge 文件不存在: ${output.absolutePath}
            💡 请先运行: ./gradlew buildWebBridge
        """.trimIndent())
    }
}

/**
 * web-bridge 构建任务
 * 符合 Gradle Configuration Cache 规范
 */
val buildWebBridge = tasks.register<Exec>("buildWebBridge") {
    group = "build"
    description = "构建 web-bridge TypeScript 项目"

    val webBridgeDir = file("../public/embedded/sub-projects/web-bridge")
    val webBridgeOutputFile = file("$webBridgeDir/dist/slax-reader-web-bridge.js")

    workingDir(webBridgeDir)

    // 跨平台兼容性处理
    val isWindows = System.getProperty("os.name").lowercase().contains("win")
    if (isWindows) {
        commandLine("cmd", "/c", "pnpm install && pnpm build")
    } else {
        commandLine("sh", "-c", "pnpm install && pnpm build")
    }

    // 声明输出，支持增量构建
    outputs.file(webBridgeOutputFile)
    outputs.upToDateWhen { webBridgeOutputFile.exists() }

    doFirst {
        println("🔨 开始构建 web-bridge 项目...")
    }

    doLast {
        if (webBridgeOutputFile.exists()) {
            val fileSizeKB = webBridgeOutputFile.length() / 1024
            println("✅ web-bridge 构建完成 (${fileSizeKB}KB)")
        } else {
            throw GradleException("❌ 构建失败：未找到产物文件 $webBridgeOutputFile")
        }
    }
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
                    .replace("{{WEBVIEW-BRIGDE-JS}}", getWebBridgeContent())
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
    dependsOn(buildWebBridge)
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
}