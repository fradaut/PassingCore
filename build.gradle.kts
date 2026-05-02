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
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.+")
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

tasks.assemble {
    dependsOn(resourcePack)
}
