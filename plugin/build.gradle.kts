plugins {
    alias(libs.plugins.run.paper)
}

val minecraftVersion = providers.gradleProperty("minecraftVersion")

dependencies {
    compileOnly(libs.paper.api)
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

tasks.runServer {
    minecraftVersion(minecraftVersion.get())
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
