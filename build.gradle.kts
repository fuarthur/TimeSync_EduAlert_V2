// Override AGP's older KGP while retaining its built-in Kotlin integration.
// The bundled KGP calls Configuration.setVisible, deprecated in Gradle 9.8.
buildscript {
    dependencies {
        classpath(libs.kotlin.gradle.plugin)
    }
}

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
}
