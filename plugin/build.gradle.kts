plugins {
    alias(libs.plugins.run.paper)
}

val minecraftVersion = providers.gradleProperty("minecraftVersion")

dependencies {
    compileOnly(libs.paper.api)

    testImplementation(libs.snakeyaml)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.launcher)
}

tasks.test {
    useJUnitPlatform()
}

// Output of the pack builder (pack.zip, pack.sha1, registry.json, credits.json), embedded under lna/.
val packOutput = project(":packbuilder").layout.buildDirectory.dir("pack")

tasks.processResources {
    dependsOn(":packbuilder:buildPack")
    from(packOutput) { into("lna") }

    val props = mapOf("version" to project.version, "apiVersion" to minecraftVersion.get())
    inputs.properties(props)
    filesMatching("plugin.yml") { expand(props) }
}

tasks.jar {
    archiveBaseName.set("LassNichtsAnbrennen")
}

// ---------------------------------------------------------------------------------------------
// Development server: ./gradlew dev  (or :plugin:runServer)
// Paper, this plugin and the dev plugins below are downloaded and loaded automatically.
// ---------------------------------------------------------------------------------------------

// Accepting the Minecraft EULA is a personal decision: set lna.acceptMinecraftEula=true in
// ~/.gradle/gradle.properties (or pass -Plna.acceptMinecraftEula=true) after reading
// https://aka.ms/MinecraftEULA. Without it, the server stops on first start and asks for run/eula.txt.
val eulaAccepted = providers.gradleProperty("lna.acceptMinecraftEula").map { it.toBoolean() }.orElse(false)
val devServerDir = layout.projectDirectory.dir("run")

val prepareDevServer = tasks.register("prepareDevServer") {
    group = "lna"
    description = "Writes development defaults to run/server.properties (only if the file does not exist)"
    val properties = devServerDir.file("server.properties")
    doLast {
        val file = properties.asFile
        if (file.exists()) {
            return@doLast
        }
        file.parentFile.mkdirs()
        file.writeText(
            """
            |# Development defaults written by :plugin:prepareDevServer. Delete this file to regenerate it.
            |motd=Lass nichts anbrennen - Dev-Server
            |gamemode=creative
            |difficulty=peaceful
            |spawn-monsters=false
            |spawn-protection=0
            |level-type=minecraft\:flat
            |generator-settings={"layers"\:[{"block"\:"minecraft\:bedrock","height"\:1},{"block"\:"minecraft\:dirt","height"\:2},{"block"\:"minecraft\:grass_block","height"\:1}],"biome"\:"minecraft\:plains"}
            |generate-structures=false
            |allow-flight=true
            |white-list=false
            |enforce-whitelist=false
            |view-distance=8
            |simulation-distance=6
            |""".trimMargin()
        )
    }
}

tasks.runServer {
    minecraftVersion(minecraftVersion.get())
    dependsOn(prepareDevServer)
    downloadPlugins {
        modrinth("worldedit", providers.gradleProperty("devWorldEditVersion").get())
        modrinth("bettermodel", providers.gradleProperty("devBetterModelVersion").get())
    }
    jvmArgs("-Xms1G", "-Xmx3G")
    if (eulaAccepted.get()) {
        jvmArgs("-Dcom.mojang.eula.agree=true")
    }
}

// Fast iteration: rebuild the pack and copy it into the running test server,
// then run /lna reloadpack in game. No server restart required.
tasks.register<Copy>("devPack") {
    group = "lna"
    description = "Builds the pack and copies it to run/plugins/LassNichtsAnbrennen/"
    dependsOn(":packbuilder:buildPack")
    from(packOutput) { include("pack.zip") }
    into(layout.projectDirectory.dir("run/plugins/LassNichtsAnbrennen"))
}
