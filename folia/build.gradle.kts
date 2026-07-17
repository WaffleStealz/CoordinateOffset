import groovy.json.JsonSlurper
import io.papermc.hangarpublishplugin.model.Platforms
import java.net.URI
import java.time.Instant

plugins {
    alias(libs.plugins.hangar.publish)
    alias(libs.plugins.modrinth.minotaur)
    alias(libs.plugins.paperweight.userdev)
    alias(libs.plugins.shadow)
}

dependencies {
    paperweight.paperDevBundle(libs.versions.paper.api)
    shadow(libs.packetevents.api)

    implementation(project(":api"))
    implementation(project(":core"))
    implementation(libs.bstats.bukkit)
    implementation(libs.configlib.paper)
    implementation(libs.morepdt)

    testImplementation(libs.paper.api)
    testImplementation(libs.test.junit.jupiter)
    testRuntimeOnly(libs.test.junit.platform)
}

tasks {
    processResources {
        inputs.property("apiVersion", libs.versions.paper.apiversion)
        val placeholders = mapOf(
            "version" to version.toString(),
            "apiVersion" to libs.versions.paper.apiversion.get(),
        )
        placeholders.forEach { (k, v) -> inputs.property(k, v) }
        files(listOf("paper-plugin.yml")) {
            expand(placeholders)
        }
    }

    jar {
        enabled = false
    }

    shadowJar {

        destinationDirectory.set(rootProject.layout.projectDirectory.dir("target"))
        archiveBaseName.set("CoordinateOffset")
        archiveClassifier.set("")
        relocate("org.bstats", "${project.group}.lib.org.bstats")
        relocate("com.jeff_media", "${project.group}.lib.com.jeff_media")
        relocate("de.exlll.configlib", "${project.group}.lib.de.exlll.configlib")
        relocate("org.snakeyaml", "${project.group}.lib.org.snakeyaml")
        relocate("io.netty", "${project.group}.lib.io.netty")
        exclude("plugin.yml")
        minimize()
    }

    register<Copy>("buildSnapshot") {

        dependsOn(shadowJar)
        from(shadowJar)
        into(rootProject.layout.projectDirectory.dir("target"))
        rename { "CoordinateOffset-SNAPSHOT.jar" }
    }

    assemble {
        dependsOn(shadowJar)
        dependsOn("buildSnapshot")
    }

    test {
        useJUnitPlatform()
    }
}

hangarPublish {
    publications.register("plugin") {
        version.set(project.version as String)
        channel.set("Release")
        id.set("CoordinateOffset")
        apiKey.set(providers.environmentVariable("HANGAR_TOKEN"))
        changelog.set(providers.environmentVariable("RELEASE_CHANGELOG"))
        platforms {
            register(Platforms.PAPER) {
                jar.set(tasks.shadowJar.flatMap { it.archiveFile })

                val versionRange = providers.environmentVariable("SUPPORTED_MC_VERSIONS").get()
                if (versionRange.isEmpty()) {
                    throw GradleException("Environment variable SUPPORTED_MC_VERSIONS is not set")
                }
                platformVersions.set(listOf(versionRange))

                dependencies {
                    url("packetevents", "https://modrinth.com/plugin/packetevents") {
                        required.set(true)
                    }
                }
            }
        }
    }
}

modrinth {
    token = providers.environmentVariable("MODRINTH_TOKEN")
    projectId = "coordinateoffset"
    versionName = "CoordinateOffset ${project.version}"
    versionNumber = "${project.version}"
    versionType = "release"
    uploadFile.set(tasks.shadowJar)
    gameVersions.set(project.provider {
        gameVersionRangeToVersions(providers.environmentVariable("SUPPORTED_MC_VERSIONS").get())
    })
    loaders.addAll(listOf("folia"))
    dependencies {
        required.project("packetevents")
    }
    changelog.set(providers.environmentVariable("RELEASE_CHANGELOG"))
}

fun gameVersionRangeToVersions(range: String): List<String> {
    val url = URI("https://api.modrinth.com/v2/tag/game_version").toURL()
    val json = url.readText()
    @Suppress("UNCHECKED_CAST")
    val parsed = JsonSlurper().parseText(json) as List<Map<String, Any>>

    data class GameVersion(
        val version: String,
        val versionType: String,
        val date: Instant,
    )

    val availableVersions = parsed.map { GameVersion(
        it["version"] as String,
        it["version_type"] as String,
        Instant.parse(it["date"] as String)
    )}

    val minDate = availableVersions.first { it.version == range.split("-")[0] }.date
    val maxDate = availableVersions.first { it.version == range.split("-")[1] }.date

    return availableVersions
        .filter { it.date in minDate..maxDate && it.versionType == "release" }
        .map { it.version }
}
