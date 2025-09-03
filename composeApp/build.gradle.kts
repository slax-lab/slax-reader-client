import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    id("org.jetbrains.kotlin.native.cocoapods")
}

repositories {
    mavenCentral()
    google()
    maven("https://jogamp.org/deployment/maven")
}

kotlin {
    cocoapods {
        version = "1.0"
        summary = "Slax Reader Client"
        homepage = "https://github.com/slax-lab/slax-reader-client"
        ios.deploymentTarget = "14.1"
        source = "https://cdn.cocoapods.org"

        pod("powersync-sqlite-core") {
            linkOnly = true
        }

        framework {
            baseName = "ComposeApp"
            isStatic = true
            export("com.powersync:core")
        }
    }

    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(
        macosArm64(),
        macosX64(),
        linuxX64(),
        mingwX64()
    ).forEach { }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
            linkerOpts.add("-lsqlite3")
            freeCompilerArgs += listOf("-Xbinary=bundleId=com.slax.reader")
        }
    }

    jvm()

    sourceSets {
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation("io.ktor:ktor-client-okhttp:2.3.12")
            implementation("io.insert-koin:koin-android:4.1.0")
        }
        iosMain.dependencies {
            implementation("io.ktor:ktor-client-darwin:2.3.12")
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

            // webview
            api(libs.compose.webview.multiplatform)

            // hyper
            implementation("com.fleeksoft.ksoup:ksoup-kotlinx:0.2.5")
            implementation("com.fleeksoft.ksoup:ksoup:0.2.5")

            implementation("io.coil-kt.coil3:coil-compose:3.0.0-alpha08")
            implementation("io.coil-kt.coil3:coil-network-ktor:3.0.0-alpha08")

            // log
            implementation("io.github.aakira:napier:2.7.1")

            // htmlconverter
            implementation("be.digitalia.compose.htmlconverter:htmlconverter:1.1.0")

            // rich editor
            implementation("com.mohamedrejeb.richeditor:richeditor-compose:1.0.0-rc13")

            implementation(libs.datastore.preferences)

            // PowerSync
            api("com.powersync:core:1.5.0")

            // DI
            implementation("io.insert-koin:koin-core:4.1.0")
            implementation("io.insert-koin:koin-compose:4.1.0")

            // HTTP client (for endpoint reachability checks)
            implementation("io.ktor:ktor-client-core:2.3.12")
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            // Ktor engine for Desktop
            implementation("io.ktor:ktor-client-java:2.3.12")
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
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
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

compose.desktop {
    application {
        mainClass = "com.slax.reader.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.slax.reader"
            packageVersion = "1.0.0"

            includeAllModules = true
        }

        jvmArgs += listOf(
            "-Dkcef.bundles.dir=./kcef-bundle",
            "-Dkcef.cache.dir=./kcef-bundle/cache",
            "-Djcef.bundle.dir=./kcef-bundle",
            "--add-opens=java.desktop/sun.awt=ALL-UNNAMED",
            "--add-opens=java.desktop/java.awt.peer=ALL-UNNAMED",
            "--add-opens=java.desktop/java.awt=ALL-UNNAMED",
            "--add-opens=java.base/java.lang=ALL-UNNAMED",
            "-Xmx2048m"
        )

        buildTypes.release.proguard {
            isEnabled = false
        }
    }
}
