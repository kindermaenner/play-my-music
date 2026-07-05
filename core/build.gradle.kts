plugins {
    alias(libs.plugins.android.library)
    jacoco
}

android {
    namespace = "de.kindermaenner.playmymusic.core"

    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            enableUnitTestCoverage = true
        }
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
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}

tasks.register<JacocoReport>("jacocoTestReport") {
    description = "Generates Jacoco coverage reports for debug unit tests"
    group = "verification"

    dependsOn("testDebugUnitTest")

    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    val fileFilter = listOf(
        "**/R.class",
        "**/R$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*"
    )

    val buildDirProvider = layout.buildDirectory

    val kotlinClasses = fileTree(buildDirProvider.dir("tmp/kotlin-classes/debug")) {
        exclude(fileFilter)
    }

    val javaClasses = fileTree(buildDirProvider.dir("/intermediates/javac/debug/classes")) {
        exclude(fileFilter)
    }

    classDirectories.setFrom(files(kotlinClasses, javaClasses))

    sourceDirectories.setFrom(files("src/main/java", "src/main/kotlin"))

    executionData.setFrom(
        fileTree(buildDirProvider) {
            include("**/*.exec")
        }
    )
}

tasks.withType<Test>().configureEach {
    extensions.configure(org.gradle.testing.jacoco.plugins.JacocoTaskExtension::class) {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}