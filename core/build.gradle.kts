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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildTypes {
        debug {
        }
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

tasks.withType<Test>().configureEach {
    extensions.configure(org.gradle.testing.jacoco.plugins.JacocoTaskExtension::class) {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}

tasks.register<JacocoReport>("jacocoTestReport") {
    description = "Generates JaCoCo coverage report for debug unit tests"
    group = "verification"

    dependsOn("testDebugUnitTest")

    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    val excludes = listOf(
        "**/R.class",
        "**/R$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*"
    )

    val kotlinClassesAgp9 = fileTree("${layout.buildDirectory.get().asFile}/intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes") {
        exclude(excludes)
    }

    val javaClassesAgp9 = fileTree("${layout.buildDirectory.get().asFile}/intermediates/javac/debug/compileDebugJavaWithJavac/classes") {
        exclude(excludes)
    }

    val kotlinClassesLegacy = fileTree("${layout.buildDirectory.get().asFile}/tmp/kotlin-classes/debug") {
        exclude(excludes)
    }

    val libraryClassesJar = layout.buildDirectory.file(
        "intermediates/compile_library_classes_jar/debug/bundleLibCompileToJarDebug/classes.jar"
    )

    classDirectories.setFrom(
        files(
            kotlinClassesAgp9,
            javaClassesAgp9,
            kotlinClassesLegacy,
            providers.provider {
                val jar = libraryClassesJar.get().asFile
                if (jar.exists()) {
                    zipTree(jar).matching { exclude(excludes) }
                } else {
                    files()
                }
            }
        )
    )

    sourceDirectories.setFrom(
        files("src/main/java", "src/main/kotlin")
    )

    executionData.setFrom(layout.buildDirectory.file("jacoco/testDebugUnitTest.exec"))
}