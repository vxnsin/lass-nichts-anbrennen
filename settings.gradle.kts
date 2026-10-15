plugins {
    // Provisions a matching JDK automatically when the toolchain version is not installed.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "lass-nichts-anbrennen"

include("packbuilder", "plugin")
