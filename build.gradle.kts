import org.gradle.api.artifacts.Configuration

// Top-level build file where you can add configuration options common to all subprojects/modules.
plugins {
    id("base")
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.sonarqube)
    alias(libs.plugins.kover)
}

val plantumlConfig: Configuration = configurations.create("plantumlConfig")

dependencies {
    plantumlConfig(libs.plantuml)
}

tasks.register<JavaExec>("generateDiagrams") {
    group = "documentation"
    description = "Generates SVG diagrams from PlantUML source files."
    classpath = plantumlConfig
    mainClass.set("net.sourceforge.plantuml.Run")

    args(
        "-tsvg",
        "-o", "${projectDir}/docs/architecture/diagrams/out",
        "${projectDir}/docs/architecture/diagrams/src/*.puml"
    )

    inputs.dir("docs/architecture/diagrams/src")
    outputs.dir("docs/architecture/diagrams/out")
}

// Automatically generate diagrams during build
tasks.named("assemble") {
    dependsOn("generateDiagrams")
}

// Configuration for merged reports at root level
dependencies {
    kover(project(":app"))
    kover(project(":domain"))
    kover(project(":data"))
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
        // We use wildcards to find all JaCoCo reports in all subprojects
        property("sonar.coverage.jacoco.xmlReportPaths", "${layout.buildDirectory.get()}/reports/kover/report.xml,**/reports/jacoco/**/*.xml")
    }
}


subprojects {
    apply(plugin = "jacoco")
    apply(plugin = "org.jetbrains.kotlinx.kover")

    // Global Jacoco configuration
    extensions.configure<JacocoPluginExtension> {
        toolVersion = "0.8.12"
    }

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
            buildTypes {
                getByName("debug") {
                    enableAndroidTestCoverage = true
                }
            }
            defaultConfig {
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            }
        }

        setupJacocoAndroidTestReport()
    }

    // Konfiguration für Android-Library-Module
    pluginManager.withPlugin("com.android.library") {
        extensions.configure<com.android.build.api.dsl.LibraryExtension>("android") {
            lint {
                abortOnError = false
                checkReleaseBuilds = false
            }
            buildTypes {
                getByName("debug") {
                    enableAndroidTestCoverage = true
                }
            }
            defaultConfig {
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            }
        }

        setupJacocoAndroidTestReport()
    }
}

fun Project.setupJacocoAndroidTestReport() {
    val reportTask = tasks.register<JacocoReport>("connectedDebugAndroidTestCoverageReport") {
        dependsOn("connectedDebugAndroidTest")
        group = "Reporting"
        description = "Generate Jacoco coverage reports for instrumented tests."

        reports {
            xml.required.set(true)
            html.required.set(true)
        }

        val buildDir = layout.buildDirectory

        classDirectories.setFrom(fileTree(buildDir) {
            include("**/intermediates/**/debug/**/classes/**")
            include("**/intermediates/kotlin_classes/debug/**")
            include("**/classes/kotlin/debug/**")
            exclude("**/R.class", "**/R$*.class", "**/BuildConfig.*", "**/Manifest*.*")
        })

        sourceDirectories.setFrom(files("$projectDir/src/main/java", "$projectDir/src/main/kotlin"))

        executionData.setFrom(fileTree(buildDir) {
            include("**/outputs/code_coverage/**/*.ec")
            include("**/outputs/connected_android_test_additional_output/**/*.ec")
        })

        // Skip the task if no execution data is found (e.g. module has no instrumented tests)
        onlyIf {
            executionData.asFileTree.any { it.exists() }
        }
    }

    // Ensure the report is generated automatically after tests are run
    tasks.matching { it.name == "connectedDebugAndroidTest" }.configureEach {
        finalizedBy(reportTask)
    }
}
