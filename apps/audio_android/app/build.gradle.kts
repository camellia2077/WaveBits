import com.mikepenz.aboutlibraries.plugin.AboutLibrariesExtension
import org.gradle.kotlin.dsl.configure
import java.util.Properties

val wavebitsAndroidModulesSmokeEnabled =
    providers.gradleProperty("wavebits.android.modulesSmoke")
        .orNull
        ?.equals("true", ignoreCase = true)
        ?: false

val releaseSigningProperties = Properties().apply {
    val signingPropertiesFile = project.file("release-signing.properties")
    if (signingPropertiesFile.exists()) {
        signingPropertiesFile.inputStream().use(::load)
    }
}

fun signingProperty(name: String): String =
    releaseSigningProperties.getProperty(name)
        ?.takeIf { it.isNotBlank() }
        ?: error("Missing release signing property: $name")

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.mikepenz.aboutlibraries.plugin")
}

android {
    namespace = "com.bag.audioandroid"
    compileSdk = 35
    ndkVersion = "28.2.13676358"

    defaultConfig {
        applicationId = "com.bag.audioandroid"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "0.2.1"
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
        externalNativeBuild {
            cmake {
                val cmakeArguments = mutableListOf(
                    "-DWAVEBITS_ANDROID_MODULES_SMOKE=${
                        if (wavebitsAndroidModulesSmokeEnabled) "ON" else "OFF"
                    }"
                )
                arguments += cmakeArguments
            }
        }
    }

    signingConfigs {
        create("release") {
            storeFile = project.file(signingProperty("storeFile"))
            storePassword = signingProperty("storePassword")
            keyAlias = signingProperty("keyAlias")
            keyPassword = signingProperty("keyPassword")
        }
    }

    buildTypes {
        debug {
            // Keep x86_64 available for local emulator/debug builds while
            // release stays arm64-only.
            ndk {
                abiFilters += listOf("x86_64")
            }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            isJniDebuggable = false
            signingConfig = signingConfigs.getByName("release")
            ndk {
                debugSymbolLevel = "NONE"
            }
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

    buildFeatures {
        compose = true
        buildConfig = true
    }

    androidResources {
        localeFilters += listOf("en", "zh", "zh-rTW", "ja")
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "4.1.2"
        }
    }

    sourceSets {
        getByName("main").res.directories.add("build/generated/aboutlibraries/res")
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

extensions.configure<AboutLibrariesExtension>("aboutLibraries") {
    android {
        registerAndroidTasks.set(false)
    }
    export {
        outputFile.set(layout.buildDirectory.file("generated/aboutlibraries/res/raw/aboutlibraries.json"))
    }
}

tasks.named("preBuild").configure {
    dependsOn("exportLibraryDefinitions")
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.profileinstaller:profileinstaller:1.4.0")
    implementation("androidx.compose.ui:ui:1.6.8")
    implementation("androidx.compose.ui:ui-tooling-preview:1.6.8")
    implementation("androidx.compose.material:material-icons-extended:1.6.8")
    implementation("androidx.compose.material3:material3:1.2.1")
    implementation("com.mikepenz:aboutlibraries-core:12.2.4")
    implementation("com.mikepenz:aboutlibraries-compose-m3:12.2.4")
    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("org.robolectric:robolectric:4.14.1")
    debugImplementation("androidx.compose.ui:ui-tooling:1.6.8")
}
