plugins {
    id("com.android.application")
}

val signingVariables = listOf(
    "RELEASE_KEYSTORE", "RELEASE_KEY_ALIAS",
    "RELEASE_STORE_PASSWORD", "RELEASE_KEY_PASSWORD"
)
val signingValues = signingVariables.associateWith { providers.environmentVariable(it).orNull }
val hasReleaseSigning = signingValues.values.all { !it.isNullOrBlank() }
require(signingValues.values.all { it.isNullOrBlank() } || hasReleaseSigning) {
    "Release signing requires all four RELEASE_* environment variables; see docs/BUILDING.md."
}

android {
    namespace = "io.github.li_yifei.chromeautofillbridge"
    compileSdk = 36
    buildToolsVersion = "36.1.0"

    defaultConfig {
        applicationId = "io.github.li_yifei.chromeautofillbridge"
        minSdk = 30
        targetSdk = 36
        versionCode = 3
        versionName = "0.4.0"
    }
    if (hasReleaseSigning) {
        signingConfigs.create("release") {
            storeFile = rootProject.file(signingValues.getValue("RELEASE_KEYSTORE")!!)
            keyAlias = signingValues.getValue("RELEASE_KEY_ALIAS")
            storePassword = signingValues.getValue("RELEASE_STORE_PASSWORD")
            keyPassword = signingValues.getValue("RELEASE_KEY_PASSWORD")
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    compileOnly("io.github.libxposed:api:101.0.0")
}
