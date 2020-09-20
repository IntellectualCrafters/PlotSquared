import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

repositories {
    maven {
        name = "Jitpack"
        url = uri("https://jitpack.io")
    }
    maven {
        name = "IntellectualSites"
        url = uri("https://mvn.intellectualsites.com/content/repositories/snapshots")
    }
    maven {
        name = "Sonatype OSS"
        url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
    }
}
var textVersion = "3.0.2"

dependencies {
    "implementation"("org.yaml:snakeyaml:1.26")
    "implementation"("com.google.code.gson:gson:2.8.6") {
        because("Minecraft uses GSON 2.8.0")
    }
    "implementation"("org.jetbrains.kotlin:kotlin-stdlib:1.3.72")
    "implementation"("org.khelekore:prtree:1.7.0-SNAPSHOT")
    // Adventure related stuff
    "implementation"("net.kyori:adventure-api:4.0.0-SNAPSHOT")
    "implementation"("net.kyori:adventure-text-minimessage:4.0.0-SNAPSHOT")
    "compile"("com.google.inject:guice:4.2.3")
    "compile"("com.google.inject.extensions:guice-assistedinject:4.2.3")
    "compile"("com.google.code.findbugs:annotations:3.0.1")
    "compile"("javax.inject:javax.inject:1")
    "compile"("aopalliance:aopalliance:1.0")
    // logging
    "implementation"("org.apache.logging.log4j:log4j-slf4j-impl:2.8.1")
    "implementation"("com.intellectualsites:Pipeline:1.4.0-SNAPSHOT")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.register<Copy>("copyFiles") {
    doLast {
        copy {
            from("../mvn/com/plotsquared/PlotSquared-Core/${project.version}/")
            into("../mvn/com/plotsquared/PlotSquared-Core/latest/")
            include("PlotSquared-Core*.jar")
            rename("PlotSquared-Core-${project.version}.jar", "PlotSquared-Core-latest.jar")
        }
    }
}

tasks.named<ShadowJar>("shadowJar") {
    dependencies {
        include(dependency("net.kyori:adventure-api:4.0.0-SNAPSHOT"))
        include(dependency("net.kyori:adventure-gson:4.0.0-SNAPSHOT"))
        include(dependency("net.kyori:adventure-legacy:4.0.0-SNAPSHOT"))
        include(dependency("net.kyori:adventure-plain:4.0.0-SNAPSHOT"))
        include(dependency("net.kyori:adventure-text-minimessage:4.0.0-SNAPSHOT"))
        include(dependency("org.khelekore:prtree:1.7.0-SNAPSHOT"))
        relocate("net.kyori.text", "com.plotsquared.formatting.text")
        relocate("org.json", "com.plotsquared.json") {
            exclude("org/json/simple/**")
        }
    }
}


tasks.named("build").configure {
    dependsOn("shadowJar")
}

tasks.named("build").configure {
    finalizedBy("copyFiles")
}

tasks.named("copyFiles").configure {
    dependsOn("createPom")
}