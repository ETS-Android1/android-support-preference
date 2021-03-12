plugins {
    id("com.android.library")
}

android {
    compileSdkVersion(30)

    defaultConfig {
        minSdkVersion(14)
    }

    lintOptions {
        isCheckReleaseBuilds = false
        isAbortOnError = false
        // Revert when lint stops with all the false positives >:-(
    }
}

val metalava by configurations.creating

dependencies {
    implementation("androidx.annotation:annotation:1.1.0")

    api("androidx.appcompat:appcompat:1.1.0")

    // Metalava isn't released yet. Check in its jar and explicitly track its transitive deps.
    metalava(rootProject.files("metalava.jar"))
    metalava("com.android.tools.external.org-jetbrains:uast:27.2.0-alpha11")
    metalava("com.android.tools.external.com-intellij:kotlin-compiler:27.2.0-alpha11")
    metalava("com.android.tools.external.com-intellij:intellij-core:27.2.0-alpha11")
    metalava("com.android.tools.lint:lint-api:27.2.0-alpha11")
    metalava("com.android.tools.lint:lint-checks:27.2.0-alpha11")
    metalava("com.android.tools.lint:lint-gradle:27.2.0-alpha11")
    metalava("com.android.tools.lint:lint:27.2.0-alpha11")
    metalava("com.android.tools:common:27.2.0-alpha11")
    metalava("com.android.tools:sdk-common:27.2.0-alpha11")
    metalava("com.android.tools:sdklib:27.2.0-alpha11")
    metalava("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.4.30")
    metalava("org.jetbrains.kotlin:kotlin-reflect:1.4.30")
    metalava("org.ow2.asm:asm:8.0")
    metalava("org.ow2.asm:asm-tree:8.0")
}

group = rootProject.property("GROUP_ID").toString()
version = rootProject.property("VERSION_NAME").toString()

apply(from = rootProject.file("android-metalava.gradle"))
