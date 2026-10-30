import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

// Staged Play launch (docs/SPEC.md): background location is only compiled in when this is true.
val backgroundLocation = providers.gradleProperty(
    "ehligible.backgroundLocation",
).map(String::toBoolean).getOrElse(false)
val appVersionName = providers.gradleProperty("ehligible.versionName").getOrElse("1.0.0")

// Release signing (Play upload key). CI passes EHLIGIBLE_* environment variables; locally the values come from
// ~/.ehligible/keystore.properties. Neither is ever committed. Without them, release builds are unsigned.
val keystoreProperties = Properties().apply {
    val path = providers.gradleProperty("ehligible.keystoreProperties").orNull
        ?: "${System.getProperty("user.home")}/.ehligible/keystore.properties"
    file(path).takeIf { it.exists() }?.inputStream()?.use { load(it) }
}

fun signingValue(key: String, env: String): String? =
    providers.environmentVariable(env).orNull ?: keystoreProperties.getProperty(key)

android {
    namespace = "io.github.mirhamedjz.ehligible"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        // Public (the Play Store link); the code's package names above are internal and unchanged.
        applicationId = "io.github.ehligible.app"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        // CI sets the version code from the run number; local builds use 1.
        versionCode = providers.gradleProperty("ehligible.versionCode").map(String::toInt).getOrElse(1)
        versionName = appVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("boolean", "BACKGROUND_LOCATION", backgroundLocation.toString())
    }

    signingConfigs {
        create("release") {
            signingValue("storeFile", "EHLIGIBLE_KEYSTORE_FILE")?.let { storeFile = file(it) }
            storePassword = signingValue("storePassword", "EHLIGIBLE_KEYSTORE_PASSWORD")
            keyAlias = signingValue("keyAlias", "EHLIGIBLE_KEY_ALIAS")
            keyPassword = signingValue("keyPassword", "EHLIGIBLE_KEY_PASSWORD")
        }
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfigs.getByName("release").takeIf { it.storeFile != null }?.let { signingConfig = it }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    lint {
        abortOnError = true
        checkDependencies = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

androidComponents {
    onVariants { variant ->
        // Adds ACCESS_BACKGROUND_LOCATION, so builds without the switch can never declare it.
        if (backgroundLocation) {
            variant.sources.manifests.addStaticManifestFile("src/backgroundLocation/AndroidManifest.xml")
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(projects.composeApp)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.compose.ui)
    implementation(libs.compose.uiToolingPreview)
    implementation(libs.compose.components.resources)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.work.runtime)
    implementation(libs.play.services.location)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.okio)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}
