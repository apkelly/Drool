plugins {
    id("org.jetbrains.kotlin.jvm")
}

dependencies {
    testImplementation(libs.konsist)
    testImplementation(kotlin("test-junit"))
}

tasks.test {
    workingDir = rootProject.projectDir
    outputs.upToDateWhen { false }
}
