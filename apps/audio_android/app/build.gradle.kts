import com.mikepenz.aboutlibraries.plugin.AboutLibrariesExtension
import org.gradle.api.tasks.Exec
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

fun requiredSigningProperty(name: String): String = signingProperty(name) ?: error("Missing release signing property: $name")

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

val repoRootDir = rootProject.projectDir.parentFile.parentFile
val translateRunScript = repoRootDir.resolve("tools/repo_tooling/android_translate/run.py")
val translateLintBaselineFile = repoRootDir.resolve("temp/translations/lint-baseline.json")
val pythonCommand =
    if (System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) {
        listOf("py", "-3")
    } else {
        listOf("python3")
    }

plugins {
    id("com.android.application")
    id("org.jlleitschuh.gradle.ktlint")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.mikepenz.aboutlibraries.plugin")
}

android {
    namespace = "com.bag.audioandroid"
    compileSdk = 36
    compileSdkExtension = 1
    ndkVersion = "29.0.14206865"

    defaultConfig {
        applicationId = "com.bag.audioandroid"
        minSdk = 29
        targetSdk = 36
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
        localeFilters +=
            listOf(
                "en",
                "zh",
                "zh-rTW",
                "ja",
                "de",
                "ru",
                "es",
                "pt-rBR",
                "uk",
                "ko",
                "fr",
                "it",
                "pl",
                "la",
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

fun registerFlipBitsApkRename(
    variantName: String,
    assembleTaskName: String,
) {
    val variantDirectoryName = variantName.lowercase()
    val renameTask =
        tasks.register("rename${variantName}ApkToFlipBits") {
            group = "build"
            description = "Copies the $variantName APK artifact to use the FlipBits product name."
            doLast {
                val apkDirectory =
                    layout
                        .buildDirectory
                        .dir("outputs/apk/$variantDirectoryName")
                        .get()
                        .asFile
                val sourceApk = apkDirectory.resolve("app-$variantDirectoryName.apk")
                val targetApk = apkDirectory.resolve("FlipBits-$variantDirectoryName.apk")
                if (sourceApk.exists()) {
                    sourceApk.copyTo(targetApk, overwrite = true)
                }
            }
        }
    tasks.matching { it.name == assembleTaskName }.configureEach {
        finalizedBy(renameTask)
    }
}

registerFlipBitsApkRename(variantName = "Staging", assembleTaskName = "assembleStaging")
registerFlipBitsApkRename(variantName = "Release", assembleTaskName = "assembleRelease")

if (!hasReleaseSigningProperties) {
    logger.lifecycle(
        "Release signing properties are not configured; debug builds stay available, " +
            "and release signing is enabled only in local/release environments.",
    )
}

extensions.configure<AboutLibrariesExtension>("aboutLibraries") {
    collect {
        filterVariants.add("release")
    }
    export {
        variant.set("release")
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

tasks.named("preBuild").configure {
    dependsOn("exportLibraryDefinitions")
    dependsOn(checkTranslationKeyAlignment)
    dependsOn(translationLintWarn)
}

val checkTranslationKeyAlignment by tasks.registering(Exec::class) {
    group = "verification"
    description = "Fails when localized Android text keys drift from the English values/ baseline."
    workingDir = repoRootDir
    // Design intent:
    // - English `res/values` is the sole structural baseline for translation keys.
    // - High Gothic (`values-la`) is a localized style language, not an alternate baseline.
    // - Pro `_ascii_` sample strings are protocol inputs and are intentionally excluded
    //   from missing-translation checks because Pro mode only accepts ASCII payloads.
    commandLine(*(pythonCommand + listOf(translateRunScript.absolutePath, "key-alignment", "--quiet")).toTypedArray())
}

val translationLintWarn by tasks.registering(Exec::class) {
    group = "verification"
    description = "Runs translation lint in non-blocking mode for local/debug loops."
    workingDir = repoRootDir
    isIgnoreExitValue = true
    commandLine(*(pythonCommand + listOf(translateRunScript.absolutePath, "lint")).toTypedArray())
    doLast {
        val code = executionResult.get().exitValue
        if (code != 0) {
            logger.warn(
                "Translation lint reported issues (exit=$code). " +
                    "Debug/local build continues; release build will fail on translationLintStrict.",
            )
        }
    }
}

val translationLintStrict by tasks.registering(Exec::class) {
    group = "verification"
    description = "Runs translation lint in blocking mode for release builds."
    workingDir = repoRootDir
    commandLine(
        *(
            pythonCommand +
                listOf(
                    translateRunScript.absolutePath,
                    "lint",
                    "--fail-on-new",
                    "--baseline-file",
                    translateLintBaselineFile.absolutePath,
                )
        ).toTypedArray(),
    )
}

val detektCliClasspath by configurations.creating

val detekt by tasks.registering(JavaExec::class) {
    group = "verification"
    description = "Runs detekt via CLI to avoid deprecated Gradle reporting APIs in the legacy detekt Gradle plugin."
    classpath = detektCliClasspath
    mainClass.set("io.gitlab.arturbosch.detekt.cli.Main")
    workingDir = projectDir
    args(
        "--build-upon-default-config",
        "--parallel",
        "--config",
        project.file("detekt.yml").absolutePath,
        "--base-path",
        projectDir.absolutePath,
        "--input",
        listOf("src/main/java", "src/test/java").joinToString(",") { project.file(it).absolutePath },
        "--excludes",
        "**/build/**,**/generated/**",
        "--jvm-target",
        "17",
    )
}

tasks.named("check").configure {
    dependsOn(checkTranslationKeyAlignment)
    dependsOn(translationLintWarn)
    dependsOn(detekt)
}

tasks.matching { it.name == "assembleRelease" }.configureEach {
    dependsOn(translationLintStrict)
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.04.01")

    implementation(composeBom)
    androidTestImplementation(composeBom)
    testImplementation(composeBom)
    debugImplementation(composeBom)

    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.core:core-ktx:1.18.0")
    implementation("androidx.datastore:datastore-preferences:1.2.1")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.profileinstaller:profileinstaller:1.4.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.material3:material3")
    implementation("com.mikepenz:aboutlibraries-core:14.2.0")
    implementation("com.mikepenz:aboutlibraries-compose-m3:14.2.0")
    detektCliClasspath("io.gitlab.arturbosch.detekt:detekt-cli:1.23.8")
    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.test:core:1.7.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("org.robolectric:robolectric:4.16")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
