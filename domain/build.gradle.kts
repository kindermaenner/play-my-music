plugins {
    `java-library`
    kotlin("jvm")
}

kotlin {
    jvmToolchain(11)
}

dependencies {
    // Pure Kotlin/JVM dependencies only
    testImplementation(libs.junit)
}
