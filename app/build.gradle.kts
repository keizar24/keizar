plugins {
    id("org.jetbrains.compose")
    id("com.android.application")
    kotlin("android")
    kotlin("plugin.serialization")
    id("kotlinx-atomicfu")
    id("kotlin-parcelize")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.material)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.material3.window.size.class0)

    implementation(libs.atomicfu)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.viewbinding)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.icons.extended)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(projects.protocol)
    implementation(project(":client"))
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.browser)
    implementation(libs.accompanist.pager.indicators)
    implementation(libs.datastore.preferences)
    implementation(libs.coil)
    implementation(libs.coil.svg)
    implementation(libs.coil.compose)
    implementation(libs.okhttp)
//    implementation(libs.coil.network.ktor)
    implementation(projects.utils.slf4jKt)
    runtimeOnly(libs.slf4j.android)

    implementation(libs.kotlinx.serialization.protobuf)

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.client.auth)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.koin.android)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(projects.aiEngine)
    implementation(projects.ruleEngine)
    implementation(projects.client)
    implementation(projects.utils.coroutines)
    implementation(libs.stacktrace.decoroutinator.android)
}

android {
    namespace = "org.keizar.android"
    compileSdk = getIntProperty("android.compile.sdk")
    defaultConfig {
        applicationId = "org.keizar.android"
        minSdk = getIntProperty("android.min.sdk")
        targetSdk = getIntProperty("android.compile.sdk")
        versionCode = getIntProperty("android.version.code")
        versionName = project.version.toString()
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.jetpack.compose.compiler.get()
    }
    signingConfigs {
        kotlin.runCatching { getProperty("signing_release_storeFileFromRoot") }.getOrNull()?.let {
            create("release") {
                storeFile = rootProject.file(it)
                storePassword = getProperty("signing_release_storePassword")
                keyAlias = getProperty("signing_release_keyAlias")
                keyPassword = getProperty("signing_release_keyPassword")
            }
        }
        kotlin.runCatching { getProperty("signing_release_storeFile") }.getOrNull()?.let {
            create("release") {
                storeFile = file(it)
                storePassword = getProperty("signing_release_storePassword")
                keyAlias = getProperty("signing_release_keyAlias")
                keyPassword = getProperty("signing_release_keyPassword")
            }
        }
    }
    buildTypes {
        val endpoint = getPropertyOrNull("keizar.server.endpoint") ?: "http://home.him188.moe:4392"
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                *sharedAndroidProguardRules(),
            )
            buildConfigField("boolean", "ENABLE_AI_DELAY", "true")
            buildConfigField("String", "SERVER_ENDPOINT", "\"$endpoint\"")
        }
        create("debugMinified") {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                *sharedAndroidProguardRules(),
            )
            buildConfigField("boolean", "ENABLE_AI_DELAY", "false")
            buildConfigField("String", "SERVER_ENDPOINT", "\"$endpoint\"")
            signingConfig = signingConfigs.getByName("debug")
        }
        debug {
            isMinifyEnabled = false
            buildConfigField("boolean", "ENABLE_AI_DELAY", "false")
            buildConfigField("String", "SERVER_ENDPOINT", "\"$endpoint\"")
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}
