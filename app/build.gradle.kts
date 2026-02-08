import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

fun projectProp(name: String, defaultValue: String = ""): String {
    val fromGradle = providers.gradleProperty(name).orNull
    if (!fromGradle.isNullOrBlank()) return fromGradle
    return localProperties.getProperty(name, defaultValue)
}

fun toBuildConfigString(value: String): String {
    return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}

android {
    namespace = "dev.cuza.FitSync"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.cuza.FitSync"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val stravaClientId = projectProp("STRAVA_CLIENT_ID")
        val stravaClientSecret = projectProp("STRAVA_CLIENT_SECRET")
        val stravaRedirectScheme = projectProp("STRAVA_REDIRECT_SCHEME", "sh2s")
        val stravaRedirectHost = projectProp("STRAVA_REDIRECT_HOST", "oauth")

        buildConfigField("String", "STRAVA_CLIENT_ID", toBuildConfigString(stravaClientId))
        buildConfigField("String", "STRAVA_CLIENT_SECRET", toBuildConfigString(stravaClientSecret))
        buildConfigField("String", "STRAVA_REDIRECT_SCHEME", toBuildConfigString(stravaRedirectScheme))
        buildConfigField("String", "STRAVA_REDIRECT_HOST", toBuildConfigString(stravaRedirectHost))

        manifestPlaceholders["stravaRedirectScheme"] = stravaRedirectScheme
        manifestPlaceholders["stravaRedirectHost"] = stravaRedirectHost
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.org.jetbrains.kotlinx.coroutines.android)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)

    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.health.connect)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.security.crypto)

    implementation(libs.retrofit)
    implementation(libs.retrofit.moshi)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
