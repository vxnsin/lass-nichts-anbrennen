plugins {
    application
}

val contentDir = rootProject.layout.projectDirectory.dir("content")
val packOutput = project(":packbuilder").layout.buildDirectory.dir("pack")

dependencies {
    implementation(libs.minestom)
    implementation(libs.adventure.minimessage)
    implementation(libs.snakeyaml)
    implementation(libs.logback)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.launcher)
}

application {
    mainClass.set("dev.vensin.lna.server.Main")
}

tasks.test {
    useJUnitPlatform()
    inputs.dir(contentDir.dir("maps")).withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.files(contentDir.file("missions.yml"), contentDir.file("items.yml"))
        .withPathSensitivity(PathSensitivity.RELATIVE)
}

tasks.processResources {
    dependsOn(":packbuilder:buildPack")
    from(packOutput) { into("lna") }
    from(contentDir.dir("maps")) { into("maps") }
    from(contentDir.dir("lang")) { into("lang") }
    from(contentDir.file("missions.yml"))

    val props = mapOf("version" to project.version, "minecraft" to providers.gradleProperty("minecraftVersion").get())
    inputs.properties(props)
    filesMatching("version.properties") { expand(props) }
}

tasks.named<JavaExec>("run") {
    workingDir = layout.projectDirectory.dir("run").asFile
    doFirst { workingDir.mkdirs() }
    jvmArgs("-Dstdout.encoding=UTF-8", "--enable-native-access=ALL-UNNAMED")
    standardInput = System.`in`
}

// One runnable file: the server classes plus every dependency.
tasks.jar {
    archiveBaseName.set("LassNichtsAnbrennen")
    manifest {
        attributes("Main-Class" to "dev.vensin.lna.server.Main", "Implementation-Version" to project.version,
            "Enable-Native-Access" to "ALL-UNNAMED")
    }
    val runtime = configurations.runtimeClasspath
    dependsOn(runtime)
    from(runtime.map { files -> files.filter { it.name.endsWith(".jar") }.map { zipTree(it) } })
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "module-info.class")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
