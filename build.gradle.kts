plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.kotlinxSerialization) apply false
    alias(libs.plugins.spotless)
}

// CI boots an available iPhone simulator and passes its id: Kotlin's default device may not exist on that Xcode.
providers.gradleProperty("ehligible.simulator").orNull?.let { simulator ->
    subprojects {
        tasks.withType<org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeSimulatorTest>().configureEach {
            device.set(simulator)
            standalone.set(false)
        }
    }
}

spotless {
    val ktlintVersion = libs.versions.ktlint.get()
    // Scoped to source folders so Spotless never walks build outputs another task is writing.
    kotlin {
        target("*/src/**/*.kt")
        ktlint(ktlintVersion)
    }
    kotlinGradle {
        target("*.gradle.kts", "*/*.gradle.kts")
        ktlint(ktlintVersion)
    }
}
