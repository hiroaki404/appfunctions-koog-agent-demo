import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

/** Reads a key from the (gitignored) local.properties file, or "" if absent. */
fun localProperty(key: String): String {
    val properties = Properties()
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { properties.load(it) }
    }
    return properties.getProperty(key, "")
}

android {
    namespace = "dev.hiroaki404.appfunctions.koogdemo.agent"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "dev.hiroaki404.appfunctions.koogdemo.agent"
        minSdk = 36
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "GEMINI_API_KEY", "\"${localProperty("GEMINI_API_KEY")}\"")
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        // Koog 1.2.0's inline functions (e.g. typeToken) are compiled targeting JVM 17.
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    // Client-side only: AppFunctionManager, metadata types, AppFunctionData. The
    // appfunctions-compiler (KSP) is not needed here — it only generates code for
    // @AppFunctionServiceEntryPoint declarations, which belong to the tool app.
    implementation(libs.appfunctions)
    implementation(libs.koog.agents)
    implementation(libs.koog.prompt.executor.google.client)
    implementation(libs.koog.http.client.ktor)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}