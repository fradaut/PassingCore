import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.tasks.Copy

plugins {
    id("java")
}

group = "tw.sac"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.129-stable")
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

tasks.test {
    useJUnitPlatform()
}

val syncResourceImages by tasks.registering(Copy::class) {
    from("Resource/p_coin.png") {
        rename { "pack.png" }
        into("")
    }
    from("Resource/p_coin.png") {
        rename { "p_coin.png" }
        into("assets/passingcore/textures/item")
    }
    from("Resource/fly_potion.png") {
        rename { "fly_potion.png" }
        into("assets/passingcore/textures/item")
    }
    from("Resource/icon_fly_potion.png") {
        rename { "icon_fly_potion.png" }
        into("assets/passingcore/textures/ui")
    }
    into("resourcepack")
}

val resourcePack by tasks.registering(Zip::class) {
    dependsOn(syncResourceImages)
    archiveFileName.set("PassingCore-resourcepack.zip")
    destinationDirectory.set(layout.buildDirectory.dir("libs"))

    from("resourcepack") {
        include("pack.mcmeta")
        include("pack.png")
        include("assets/**")
        exclude("**/.DS_Store")
    }
}

tasks.processResources {
    dependsOn(resourcePack)
    from(resourcePack.flatMap { it.archiveFile }) {
        rename { "PassingCore-resourcepack.zip" }
    }
}

val localServerDirectory = providers.gradleProperty("passingCoreServerDir")
    .orElse("/Users/fradaut/Run/MinecraftServer/paperMC_26.2#129")

val deployLocalServer by tasks.registering(Copy::class) {
    dependsOn(tasks.jar, resourcePack)
    group = "deployment"
    description = "Deploys the plugin and resource pack to the local Paper server."
    onlyIf {
        val serverDirectory = file(localServerDirectory.get())
        if (!serverDirectory.isDirectory) {
            logger.lifecycle("Skipping local server deployment: ${serverDirectory.path} does not exist.")
            false
        } else {
            true
        }
    }

    into(localServerDirectory.map { file(it).resolve("plugins") })
    from(tasks.jar.flatMap { it.archiveFile })
    from(resourcePack.flatMap { it.archiveFile }) {
        into("PassingCore")
        rename { "resourcepack.zip" }
    }
    from("src/main/resources/config.yml") {
        into("PassingCore")
    }
}

tasks.assemble {
    dependsOn(resourcePack)
}

tasks.build {
    finalizedBy(deployLocalServer)
}
