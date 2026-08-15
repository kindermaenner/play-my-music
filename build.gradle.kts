// Top-level build file where you can add configuration options common to all subprojects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.sonarqube)
    alias(libs.plugins.kover)
}

// Configuration for merged reports at root level
dependencies {
    kover(project(":app"))
    kover(project(":core"))
}

kover {
    reports {
        filters {
            excludes {
                classes(
                    "**/R.class", "**/R$*.class", "**/BuildConfig.*", "**/Manifest*.*",
                    "android/**/*.*", "**/databinding/*", "**/androidx/*",
                    "**/*MapperImpl*.*", "**/*Component*.*", "**/*BR*.*",
                    "**/*Module*.*", "**/*Dagger*.*", "**/*Hilt*.*",
                    "**/*_Factory*.*", "**/*_Provide*Factory*.*"
                )
            }
        }
    }
}

sonar {
    properties {
        property("sonar.projectKey", "play-my-music")
        property("sonar.projectName", "play-my-music")
        property("sonar.host.url", System.getenv("SONAR_HOST_URL") ?: "http://localhost:9000")
        property("sonar.token", System.getenv("SONAR_TOKEN") ?: "")
        
        // Point Sonar to Kover's aggregated XML report and JaCoCo instrumented reports
        property("sonar.coverage.jacoco.xmlReportPaths", listOf(
            "${layout.buildDirectory.get()}/reports/kover/report.xml",
            "${project(":app").layout.buildDirectory.get()}/reports/jacoco/connectedDebugAndroidTestCoverageReport/connectedDebugAndroidTestCoverageReport.xml",
            "${project(":core").layout.buildDirectory.get()}/reports/jacoco/connectedDebugAndroidTestCoverageReport/connectedDebugAndroidTestCoverageReport.xml"
        ).joinToString(","))
    }
}


subprojects {
    apply(plugin = "org.jetbrains.kotlinx.kover")

    pluginManager.withPlugin("org.sonarqube") {
        extensions.configure<org.sonarqube.gradle.SonarExtension> {
            properties {
                val lintReport = layout.buildDirectory
                    .file("reports/lint-results-debug.xml")
                    .get()
                    .asFile

                property(
                    "sonar.androidLint.reportPaths",
                    lintReport.absolutePath
                )
            }
        }
    }

    // Konfiguration für Android-App-Module
    pluginManager.withPlugin("com.android.application") {
        extensions.configure<com.android.build.api.dsl.ApplicationExtension>("android") {
            lint {
                abortOnError = false
                checkReleaseBuilds = false
            }
        }
    }

    // Konfiguration für Android-Library-Module
    pluginManager.withPlugin("com.android.library") {
        extensions.configure<com.android.build.api.dsl.LibraryExtension>("android") {
            lint {
                abortOnError = false
                checkReleaseBuilds = false
            }
        }
    }
}