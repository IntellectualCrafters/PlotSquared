import java.time.format.DateTimeFormatter

dependencies {
    // Expected everywhere.
    compileOnlyApi(libs.checkerqual)

    // Minecraft expectations
    compileOnlyApi(libs.gson)
    compileOnly(libs.guava)

    // Platform expectations
    compileOnlyApi(libs.snakeyaml)

    // Adventure
    api(libs.adventureApi)
    api(libs.adventureMiniMessage)

    // Guice
    api(libs.guice) {
        exclude(group = "com.google.guava")
    }
    api(libs.guiceassistedinject) {
        exclude("com.google.inject", "guice")
    }
    api(libs.spotbugs)

    // Plugins
    compileOnly(libs.worldeditCore) {
        exclude(group = "bukkit-classloader-check")
        exclude(group = "mockito-core")
        exclude(group = "dummypermscompat")
    }
    testImplementation(libs.worldeditCore)
    compileOnly(libs.faweBukkit) { isTransitive = false }
    testImplementation(libs.faweCore) { isTransitive = false }

    // Logging
    compileOnlyApi(libs.log4j)

    // Database
    api(libs.hikaricp)
    api(libs.jdbiCore)
    api(libs.jdbiGuice)

    // Other libraries
    api(libs.prtree)
    api(libs.aopalliance)
    api(libs.cloudServices)
    api(libs.arkitektonika)
    api(libs.paster)
    api(libs.informativeAnnotations)
}

tasks.processResources {
    filesMatching("plugin.properties") {
        expand(
                "version" to project.version.toString(),
                "commit" to rootProject.grgit.head().abbreviatedId,
                "date" to rootProject.grgit.head().dateTime.format(DateTimeFormatter.ofPattern("yy.MM.dd"))
        )
    }

    doLast {
        copy {
            from(layout.buildDirectory.file("$rootDir/LICENSE"))
            into(layout.buildDirectory.dir("resources/main"))
        }
    }
}

tasks {
    withType<Javadoc> {
        val isRelease = if (rootProject.version.toString().endsWith("-SNAPSHOT")) "TODO" else rootProject.version.toString()
        val opt = options as StandardJavadocDocletOptions
        opt.links("https://docs.enginehub.org/javadoc/com.sk89q.worldedit/worldedit-core/" + libs.worldeditCore.get().versionConstraint.toString())
        opt.links("https://jd.advntr.dev/api/" + libs.adventureApi.get().versionConstraint.toString())
        opt.links("https://jd.advntr.dev/text-minimessage/" + libs.adventureApi.get().versionConstraint.toString())
        opt.links("https://google.github.io/guice/api-docs/" + libs.guice.get().versionConstraint.toString() + "/javadoc/")
        opt.links("https://checkerframework.org/api/")
        opt.isLinkSource = true
        opt.bottom(File("$rootDir/javadocfooter.html").readText())
        opt.isUse = true
        opt.encoding("UTF-8")
        opt.keyWords()
        opt.addStringOption("-since", isRelease)
        opt.noTimestamp()
    }
}
