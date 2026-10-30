import java.util.Properties

plugins {
    // No `kotlin.android`: AGP 9 has built-in Kotlin support and applying it is a hard error.
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "app.expressiveclock"
    compileSdk = 37

    defaultConfig {
        applicationId = "app.expressiveclock"
        minSdk = 26
        targetSdk = 36
        versionCode = 4
        versionName = "0.1.2"
    }

    /**
     * Release signing configuration, loading credentials from root-level `keystore.properties` or Gradle properties.
     * Strict enforcement: fails release builds cleanly if properties or keystore file are missing.
     */
    val keystorePropsFile = rootProject.file("keystore.properties")
    val keystoreProps = Properties()
    if (keystorePropsFile.exists()) {
        keystorePropsFile.inputStream().use { stream ->
            keystoreProps.load(stream)
        }
    }

    signingConfigs {
        create("release") {
            val storePath = keystoreProps.getProperty("clockStoreFile")
                ?: (findProperty("clockStoreFile") as String?)
                ?: error("Missing 'clockStoreFile' in keystore.properties or ~/.gradle/gradle.properties")
            val storeFileObj = rootProject.file(storePath)
            if (!storeFileObj.exists()) {
                error("Keystore file '$storePath' defined by 'clockStoreFile' does not exist.")
            }
            storeFile = storeFileObj
            storePassword = keystoreProps.getProperty("clockStorePassword")
                ?: (findProperty("clockStorePassword") as String?)
                ?: error("Missing 'clockStorePassword' in keystore.properties or ~/.gradle/gradle.properties")
            keyAlias = keystoreProps.getProperty("clockKeyAlias")
                ?: (findProperty("clockKeyAlias") as String?)
                ?: error("Missing 'clockKeyAlias' in keystore.properties or ~/.gradle/gradle.properties")
            keyPassword = keystoreProps.getProperty("clockKeyPassword")
                ?: (findProperty("clockKeyPassword") as String?)
                ?: error("Missing 'clockKeyPassword' in keystore.properties or ~/.gradle/gradle.properties")
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                // Must be the -optimize variant; plain proguard-android.txt carries -dontoptimize,
                // which is a hard error on AGP 9.
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
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
        localeFilters += listOf("en")
    }

    // No accounts, no telemetry, no ad IDs. The dependency manifest stays out of the APK too.
    dependenciesInfo {
        includeInApk = false
        includeInBundle = true
    }

    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/*.version",
                "/META-INF/com.android.tools/**",
                "/kotlin/**",
                "/DebugProbesKt.bin",
            )
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

kotlin {
    compilerOptions {
        // [M3E] Most expressive components are still opt-in even though MaterialExpressiveTheme
        // itself is not. Opting in centrally keeps the annotation noise out of every call site.
        optIn.addAll(
            "androidx.compose.material3.ExperimentalMaterial3Api",
            "androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
            "androidx.compose.foundation.ExperimentalFoundationApi",
            "androidx.compose.ui.text.ExperimentalTextApi",
        )
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.text)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.material.icons.extended)

    implementation(libs.androidx.core.ktx)
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.graphics.shapes)

    implementation("com.google.android.play:app-update:2.1.0")
    implementation("com.google.android.play:app-update-ktx:2.1.0")

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.androidx.test.core)
}
