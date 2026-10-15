dependencies {
    implementation(libs.snakeyaml)
    implementation(libs.gson)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.launcher)
}

tasks.test {
    useJUnitPlatform()
}

// Builds the resource pack from content/ and validates CREDITS.md. Output: build/pack/
val buildPack = tasks.register<JavaExec>("buildPack") {
    group = "lna"
    description = "Builds the resource pack from content/ and validates CREDITS.md"

    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("io.github.vxnsin.lna.packbuilder.Main")
    jvmArgs("-Dstdout.encoding=UTF-8", "-Dstderr.encoding=UTF-8")

    // Local values only: the argument provider is serialized into the configuration cache.
    val contentDir = rootProject.layout.projectDirectory.dir("content")
    val creditsFile = rootProject.layout.projectDirectory.file("CREDITS.md")
    val packFormat = providers.gradleProperty("resourcePackFormat")
    val outDir = layout.buildDirectory.dir("pack")
    inputs.dir(contentDir).withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.file(creditsFile).withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.property("packFormat", packFormat)
    outputs.dir(outDir)

    argumentProviders.add(CommandLineArgumentProvider {
        listOf(
            "--content", contentDir.asFile.absolutePath,
            "--credits", creditsFile.asFile.absolutePath,
            "--out", outDir.get().asFile.absolutePath,
            "--pack-format", packFormat.get(),
        )
    })
}

tasks.named("build") { dependsOn(buildPack) }
