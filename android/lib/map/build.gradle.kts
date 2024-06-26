plugins {
    id(Dependencies.Plugin.kotlinAndroidId)
    id(Dependencies.Plugin.androidLibraryId)
    id(Dependencies.Plugin.composeCompiler) version Versions.kotlin
}

android {
    namespace = "net.mullvad.mullvadvpn.lib.map"
    compileSdk = Versions.Android.compileSdkVersion

    defaultConfig { minSdk = Versions.Android.minSdkVersion }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions { jvmTarget = Versions.jvmTarget }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        lintConfig = file("${rootProject.projectDir}/config/lint.xml")
        abortOnError = true
        warningsAsErrors = true
    }
}

dependencies {
    implementation(project(Dependencies.Mullvad.modelLib))

    implementation(Dependencies.AndroidX.lifecycleRuntimeKtx)
    implementation(Dependencies.Compose.ui)
    implementation(Dependencies.Compose.foundation)
    implementation(Dependencies.kermit)
}
