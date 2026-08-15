plugins {
    alias(libs.plugins.android.library)
    id("jacoco")
}

configure<JacocoPluginExtension> {
    toolVersion = "0.8.12"
}

android {
    namespace = "de.kindermaenner.playmymusic.core"
    compileSdk = 37

    buildTypes {
        debug {
            enableAndroidTestCoverage = true
        }
    }

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    implementation(libs.gson)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}

tasks.register<JacocoReport>("connectedDebugAndroidTestCoverageReport") {
    dependsOn("connectedDebugAndroidTest")
    group = "Reporting"
    description = "Generate Jacoco coverage reports for instrumented tests."

    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    val buildDir = layout.buildDirectory

    val allDebugClasses = fileTree(buildDir) {
        include("**/intermediates/**/debug/**/classes/**")
        include("**/intermediates/kotlin_classes/debug/**")
        include("**/classes/kotlin/debug/**")
        exclude("**/R.class", "**/R$*.class", "**/BuildConfig.*", "**/Manifest*.*")
    }
    classDirectories.setFrom(allDebugClasses)

    sourceDirectories.setFrom(files("$projectDir/src/main/java", "$projectDir/src/main/kotlin"))

    executionData.setFrom(fileTree(buildDir) {
        include("**/outputs/code_coverage/**/*.ec")
        include("**/outputs/connected_android_test_additional_output/**/*.ec")
    })
}

