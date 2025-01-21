import com.diffplug.gradle.spotless.SpotlessPlugin
import com.github.jengelman.gradle.plugins.shadow.ShadowPlugin
import groovy.json.JsonSlurper
import xyz.jpenilla.runpaper.task.RunServer
import java.net.URI

plugins {
    java
    `java-library`
    `maven-publish`
    signing

    alias(libs.plugins.shadow)
    alias(libs.plugins.spotless)
    alias(libs.plugins.grgit)
    alias(libs.plugins.nexus)

    eclipse
    idea

    alias(libs.plugins.runPaper)
}

group = "com.intellectualsites.plotsquared"
version = "7.4.2-SNAPSHOT"

if (!File("$rootDir/.git").exists()) {
    logger.lifecycle("""
    **************************************************************************************
    You need to fork and clone this repository! Don't download a .zip file.
    If you need assistance, consult the GitHub docs: https://docs.github.com/get-started/quickstart/fork-a-repo
    **************************************************************************************
    """.trimIndent()
    ).also { kotlin.system.exitProcess(1) }
}

subprojects {
    group = rootProject.group
    version = rootProject.version

    repositories {
        mavenCentral()

        maven {
            name = "Sonatype OSS"
            url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
        }

        maven {
            name = "Sonatype OSS (S01)"
            url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
        }

        maven {
            name = "Jitpack"
            url = uri("https://jitpack.io")
            content {
                includeModule("com.github.MilkBowl", "VaultAPI")
            }
        }

        maven {
            name = "EngineHub"
            url = uri("https://maven.enginehub.org/repo/")
        }
    }

    apply {
        plugin<JavaPlugin>()
        plugin<JavaLibraryPlugin>()
        plugin<MavenPublishPlugin>()
        plugin<ShadowPlugin>()
        plugin<SpotlessPlugin>()
        plugin<SigningPlugin>()

        plugin<EclipsePlugin>()
        plugin<IdeaPlugin>()
    }

    dependencies {
        // Tests
        testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.4")
    }

    plugins.withId("java") {
        the<JavaPluginExtension>().toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    tasks.compileJava.configure {
        options.release.set(17)
    }

    configurations.all {
        attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 21)
    }

    spotless {
        java {
            licenseHeaderFile(rootProject.file("HEADER.txt"))
            target("**/*.java")
            endWithNewline()
            trimTrailingWhitespace()
            removeUnusedImports()
        }
    }

    java {
        withSourcesJar()
        withJavadocJar()
    }

    val javaComponent = components["java"] as AdhocComponentWithVariants
    javaComponent.withVariantsFromConfiguration(configurations["shadowRuntimeElements"]) {
        skip()
    }

    signing {
        if (!project.hasProperty("skip.signing") && !version.toString().endsWith("-SNAPSHOT")) {
            val signingKey: String? by project
            val signingPassword: String? by project
            useInMemoryPgpKeys(signingKey, signingPassword)
            signing.isRequired
            sign(publishing.publications)
        }
    }

    publishing {
        publications {
            create<MavenPublication>("maven") {
                from(components["java"])

                pom {

                    name.set(project.name + " " + project.version)
                    description.set("PlotSquared, a land and world management plugin for Minecraft.")
                    url.set("https://github.com/IntellectualSites/PlotSquared")

                    licenses {
                        license {
                            name.set("GNU General Public License, Version 3.0")
                            url.set("https://www.gnu.org/licenses/gpl-3.0.html")
                            distribution.set("repo")
                        }
                    }

                    developers {
                        developer {
                            id.set("Sauilitired")
                            name.set("Alexander Söderberg")
                            organization.set("IntellectualSites")
                            organizationUrl.set("https://github.com/IntellectualSites")
                        }
                        developer {
                            id.set("NotMyFault")
                            name.set("Alexander Brandes")
                            organization.set("IntellectualSites")
                            organizationUrl.set("https://github.com/IntellectualSites")
                            email.set("contact(at)notmyfault.dev")
                        }
                        developer {
                            id.set("SirYwell")
                            name.set("Hannes Greule")
                            organization.set("IntellectualSites")
                            organizationUrl.set("https://github.com/IntellectualSites")
                        }
                        developer {
                            id.set("dordsor21")
                            name.set("dordsor21")
                            organization.set("IntellectualSites")
                            organizationUrl.set("https://github.com/IntellectualSites")
                        }
                    }

                    scm {
                        url.set("https://github.com/IntellectualSites/PlotSquared")
                        connection.set("scm:git:https://github.com/IntellectualSites/PlotSquared.git")
                        developerConnection.set("scm:git:git@github.com:IntellectualSites/PlotSquared.git")
                        tag.set("${project.version}")
                    }

                    issueManagement {
                        system.set("GitHub")
                        url.set("https://github.com/IntellectualSites/PlotSquared/issues")
                    }
                }
            }
        }
    }

    tasks {

        compileJava {
            options.compilerArgs.add("-parameters")
            options.isDeprecation = true
            options.encoding = "UTF-8"
        }

        shadowJar {
            this.archiveClassifier.set(null as String?)
            this.archiveFileName.set("${project.name}-${project.version}.${this.archiveExtension.getOrElse("jar")}")
        }

        named("build") {
            dependsOn(named("shadowJar"))
        }
        test {
            useJUnitPlatform()
        }

        withType<AbstractArchiveTask>().configureEach {
            isPreserveFileTimestamps = false
            isReproducibleFileOrder = true
        }
    }
}

nexusPublishing {
    this.repositories {
        sonatype {
            nexusUrl.set(URI.create("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(URI.create("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
        }
    }
}

tasks.getByName<Jar>("jar") {
    enabled = false
}

val supportedVersions = listOf("1.18.2", "1.19.4", "1.20.6", "1.21.1", "1.21.3", "1.21.4")
tasks {
    register("cacheLatestFaweArtifact") {
        val lastSuccessfulBuildUrl = uri("https://ci.athion.net/job/FastAsyncWorldEdit/lastSuccessfulBuild/api/json").toURL()
        val artifact = ((JsonSlurper().parse(lastSuccessfulBuildUrl) as Map<*, *>)["artifacts"] as List<*>)
                .map { it as Map<*, *> }
                .map { it["fileName"] as String }
                .first { it -> it.contains("Bukkit") }
        project.ext["faweArtifact"] = artifact
    }

    supportedVersions.forEach {
        register<RunServer>("runServer-$it") {
            dependsOn(getByName("cacheLatestFaweArtifact"))
            minecraftVersion(it)
            pluginJars(*project(":plotsquared-bukkit").getTasksByName("shadowJar", false)
                    .map { task -> (task as Jar).archiveFile }
                    .toTypedArray())
            jvmArgs("-DPaper.IgnoreJavaVersion=true", "-Dcom.mojang.eula.agree=true")
            downloadPlugins {
                url("https://ci.athion.net/job/FastAsyncWorldEdit/lastSuccessfulBuild/artifact/artifacts/${project.ext["faweArtifact"]}")
            }
            group = "run paper"
            runDirectory.set(file("run-$it"))
        }
    }
}
