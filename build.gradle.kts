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
        
        // Point Sonar to Kover's aggregated XML report
        property("sonar.coverage.jacoco.xmlReportPaths", "${project.layout.buildDirectory.get()}/reports/kover/report.xml")
    }
}
