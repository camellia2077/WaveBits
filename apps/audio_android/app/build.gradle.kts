import com.mikepenz.aboutlibraries.plugin.AboutLibrariesExtension
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.kotlin.dsl.configure
import java.io.File
import java.util.Properties

val flipbitsAndroidModulesSmokeEnabled =
    providers
        .gradleProperty("flipbits.android.modulesSmoke")
        .orNull
        ?.equals("true", ignoreCase = true)
        ?: false

val releaseSigningProperties =
    Properties().apply {
        val configuredSigningPropertiesPath =
            providers.gradleProperty("flipbits.android.releaseSigningPropertiesFile").orNull
                ?: System.getenv("FLIPBITS_ANDROID_RELEASE_SIGNING_PROPERTIES_FILE")
        val signingPropertiesFile =
            configuredSigningPropertiesPath
                ?.takeIf { it.isNotBlank() }
                ?.let(::File)
                ?: project.file("release-signing.properties")
        if (signingPropertiesFile.exists()) {
            signingPropertiesFile.inputStream().use(::load)
        }
    }

fun signingProperty(name: String): String? =
    providers.gradleProperty("flipbits.android.releaseSigning.$name").orNull?.takeIf { it.isNotBlank() }
        ?: System.getenv("FLIPBITS_ANDROID_RELEASE_SIGNING_${name.uppercase()}")?.takeIf { it.isNotBlank() }
        ?: releaseSigningProperties.getProperty(name)?.takeIf { it.isNotBlank() }

fun requiredSigningProperty(name: String): String =
    signingProperty(name) ?: error("Missing release signing property: $name")

val hasReleaseSigningProperties =
    listOf("storeFile", "storePassword", "keyAlias", "keyPassword").all {
        signingProperty(it) != null
    }

fun requiredGradleIntProperty(name: String): Int =
    providers
        .gradleProperty(name)
        .orNull
        ?.toIntOrNull()
        ?: error("Missing or invalid Gradle integer property: $name")

fun requiredGradleStringProperty(name: String): String =
    providers
        .gradleProperty(name)
        .orNull
        ?.takeIf { it.isNotBlank() }
        ?: error("Missing or blank Gradle string property: $name")

plugins {
    id("com.android.application")
    id("org.jlleitschuh.gradle.ktlint")
    id("org.jetbrains.kotlin.plugin.compose")
    id("io.gitlab.arturbosch.detekt")
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
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Android presentation version now has a single truth source in
        // apps/audio_android/gradle.properties so release tooling, docs, and
        // agent workflows do not need to edit this larger build script.
        versionCode = requiredGradleIntProperty("flipbits.android.versionCode")
        versionName = requiredGradleStringProperty("flipbits.android.versionName")
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
        externalNativeBuild {
            cmake {
                val cmakeArguments =
                    mutableListOf(
                        "-DFLIPBITS_ANDROID_MODULES_SMOKE=${
                            if (flipbitsAndroidModulesSmokeEnabled) "ON" else "OFF"
                        }",
                    )
                arguments += cmakeArguments
            }
        }
    }

    signingConfigs {
        if (hasReleaseSigningProperties) {
            create("release") {
                storeFile = file(requiredSigningProperty("storeFile"))
                storePassword = requiredSigningProperty("storePassword")
                keyAlias = requiredSigningProperty("keyAlias")
                keyPassword = requiredSigningProperty("keyPassword")
            }
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
        create("staging") {
            initWith(getByName("release"))
            // Staging should behave like release for shrink/minify issues while
            // still being directly installable and debuggable on local devices.
            isDebuggable = true
            isJniDebuggable = true
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            isJniDebuggable = false
            if (hasReleaseSigningProperties) {
                signingConfig = signingConfigs.getByName("release")
            }
            ndk {
                debugSymbolLevel = "NONE"
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
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
        localeFilters += listOf(
            "en",
            "zh",
            "zh-rTW",
            "ja",
            "de",
            "ru",
            "es",
            "pt-rBR",
            "uk",
        )
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

if (!hasReleaseSigningProperties) {
    logger.lifecycle(
        "Release signing properties are not configured; debug builds stay available, " +
            "and release signing is enabled only in local/release environments.",
    )
}

extensions.configure<AboutLibrariesExtension>("aboutLibraries") {
    android {
        registerAndroidTasks.set(false)
    }
    export {
        outputFile.set(layout.buildDirectory.file("generated/aboutlibraries/res/raw/aboutlibraries.json"))
    }
}

ktlint {
    additionalEditorconfig.put("ktlint_standard_function-naming", "disabled")
    additionalEditorconfig.put("ktlint_standard_property-naming", "disabled")
    filter {
        exclude("**/build/**")
    }
}

extensions.configure<DetektExtension>("detekt") {
    buildUponDefaultConfig = true
    parallel = true
    allRules = false
    basePath = projectDir.absolutePath
    config.setFrom(files("detekt.yml"))
    source.setFrom(
        files(
            "src/main/java",
            "src/test/java",
        ),
    )
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    jvmTarget = "17"
    exclude("**/build/**")
    exclude("**/generated/**")
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
    testImplementation("androidx.compose.ui:ui-test-junit4:1.6.8")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.6.8")
    debugImplementation("androidx.compose.ui:ui-tooling:1.6.8")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.6.8")
}
