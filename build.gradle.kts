import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

project.group = "net.oceanias"
project.version = "1.0.0"

plugins {
    id("java-library")
    id("maven-publish")

    alias(libs.plugins.shadow)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }

    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()

    maven("https://repo.codemc.io/repository/maven-public/")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://repo.xenondevs.xyz/releases")
}

publishing {
    publications {
        create<MavenPublication>("jitpack") {
            artifact(tasks.shadowJar)

            artifact(tasks.named<Jar>("sourcesJar"))
            artifact(tasks.named<Jar>("javadocJar"))

            pom {
                withXml {
                    asNode().appendNode("repositories").apply {
                        appendNode("repository").apply {
                            appendNode("id", "codemc")
                            appendNode("url", "https://repo.codemc.io/repository/maven-public/")
                        }
                        appendNode("repository").apply {
                            appendNode("id", "papermc")
                            appendNode("url", "https://repo.papermc.io/repository/maven-public/")
                        }
                        appendNode("repository").apply {
                            appendNode("id", "sonatype")
                            appendNode("url", "https://oss.sonatype.org/content/groups/public/")
                        }
                        appendNode("repository").apply {
                            appendNode("id", "xenondevs")
                            appendNode("url", "https://repo.xenondevs.xyz/releases")
                        }
                    }
                }
            }
        }
    }
}

dependencies {
    annotationProcessor(libs.lombok)

    api(libs.commandApi)
    api(libs.invUi)
    api(libs.configLib)

    compileOnly(variantOf(libs.inventoryAccess) { classifier("remapped-mojang") })
    compileOnly(libs.lombok)
    compileOnly(libs.paper)

    implementation(libs.commonsText)
}

defaultTasks("build")

tasks {
    processResources {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

        filesMatching("paper-plugin.yml") {
            expand("version" to project.version)
        }
    }

    withType<Jar>().configureEach {
        archiveBaseName.set(rootProject.name.replaceFirstChar { char -> char.uppercase() })
    }

    jar {
        enabled = false
    }

    val apiJar = register<ShadowJar>("api") {
        archiveClassifier.set("api")

        from(sourceSets.main.get().output)

        configurations = listOf(project.configurations.runtimeClasspath.get())
    }

    val serverJar = register<ShadowJar>("server") {
        archiveClassifier.set("server")

        from(sourceSets.main.get().output)

        configurations = listOf(project.configurations.runtimeClasspath.get())

        dependencies {
            exclude(dependency("xyz.xenondevs.invui:invui-core"))
            exclude(dependency("xyz.xenondevs.invui:inventory-access-r23"))
        }
    }

    build {
        dependsOn(apiJar, serverJar)
    }
}