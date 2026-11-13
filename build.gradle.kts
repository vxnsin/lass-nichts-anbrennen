tasks.register("dev") {
    group = "lna"
    description = "Builds everything and starts the server in server/run"
    dependsOn(":server:run")
}

subprojects {
    group = "dev.vensin.lna"
    version = "0.1.0-SNAPSHOT"

    apply(plugin = "java")

    repositories {
        mavenCentral()
    }

    extensions.configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(25))
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(25)
        options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:removal", "-Xlint:unchecked"))
    }
}
