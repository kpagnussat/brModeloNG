import org.apache.tools.ant.types.Commandline

plugins {
    application
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    mainClass = "principal.Aplicacao"
}

sourceSets {
    test {
        java.setSrcDirs(listOf("test"))
        resources.setSrcDirs(listOf("test-resources"))
    }
    main {
        java.setSrcDirs(listOf("src"))
        resources.setSrcDirs(listOf("src"))
        // Forms stay beside their Java sources for NetBeans, but are not runtime resources.
        resources.exclude("**/*.java", "**/*.form")
    }
}

val dev = sourceSets.create("dev") {
    java.setSrcDirs(listOf("dev"))
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
}

configurations[dev.implementationConfigurationName].extendsFrom(configurations.implementation.get())
configurations[dev.runtimeOnlyConfigurationName].extendsFrom(configurations.runtimeOnly.get())

dependencies {
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    implementation(libs.dbus.core)
    implementation(libs.dbus.unixsocket)
    runtimeOnly(libs.slf4j.nop)
    implementation(libs.flatlaf)
    implementation(libs.flatlaf.extras)
    implementation(libs.flatlaf.intellij.themes)
    implementation(libs.jsvg) {
        version { strictly(libs.versions.jsvg.get()) }
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 21
    options.encoding = "UTF-8"
    options.compilerArgs.add("-Xlint:all")
}

tasks.jar {
    archiveFileName = "brModelo.jar"
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes(
            "Main-Class" to application.mainClass.get(),
            // FlatLaf contains Java 9+ classes under META-INF/versions.
            "Multi-Release" to "true"
        )
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }) {
        exclude("module-info.class", "META-INF/versions/**/module-info.class", "META-INF/MANIFEST.MF", "META-INF/*.SF", "META-INF/*.RSA", "META-INF/*.DSA", "META-INF/SIG-*")
    }
}

fun registerDevTask(name: String, entryPoint: String, help: String) =
    tasks.register<JavaExec>(name) {
        group = "application"
        description = help
        classpath = dev.runtimeClasspath
        mainClass = entryPoint
        javaLauncher = javaToolchains.launcherFor(java.toolchain)
        // Honor quoted paths in -Pargs as well as plain whitespace-separated arguments.
        args(Commandline.translateCommandline(providers.gradleProperty("args").getOrElse("")).toList())
    }

registerDevTask("snapDialogs", "SnapDialogs", "Render dialogs and diagrams; pass arguments with -Pargs.").configure {
    val home = layout.buildDirectory.dir("snapshot-home").get().asFile
    doFirst { home.mkdirs() }
    systemProperty("user.home", home.absolutePath)
    environment("XDG_CONFIG_HOME", File(home, "config").absolutePath)
    environment("XDG_STATE_HOME", File(home, "state").absolutePath)
    environment("XDG_DATA_HOME", File(home, "data").absolutePath)
    environment("LOCALAPPDATA", File(home, "local").absolutePath)
    environment("GSETTINGS_BACKEND", "memory")
}
registerDevTask("snapTemas", "SnapTemas", "Render the live theme gallery; pass scale and output directory with -Pargs.").configure {
    val home = layout.buildDirectory.dir("themes-snapshot-home").get().asFile
    doFirst { home.mkdirs() }
    systemProperty("user.home", home.absolutePath)
    environment("XDG_CONFIG_HOME", File(home, "config").absolutePath)
    environment("XDG_STATE_HOME", File(home, "state").absolutePath)
    environment("XDG_DATA_HOME", File(home, "data").absolutePath)
    environment("LOCALAPPDATA", File(home, "local").absolutePath)
    environment("GSETTINGS_BACKEND", "memory")
    environment("GTK_THEME", "Adwaita")
}
registerDevTask("snapTabs", "SnapTabs", "Render diagram tabs and opt-in frames; pass theme, UI scale, output and native|modernas.").configure {
    val home = layout.buildDirectory.dir("tabs-snapshot-home").get().asFile
    doFirst { home.mkdirs() }
    systemProperty("user.home", home.absolutePath)
    environment("XDG_CONFIG_HOME", File(home, "config").absolutePath)
    environment("XDG_STATE_HOME", File(home, "state").absolutePath)
    environment("XDG_DATA_HOME", File(home, "data").absolutePath)
    environment("LOCALAPPDATA", File(home, "local").absolutePath)
    environment("GSETTINGS_BACKEND", "memory")
}
registerDevTask("snapInspector", "SnapInspector", "Render every inspector property type and state; pass theme, scale, output and fixture paths.").configure {
    val home = layout.buildDirectory.dir("inspector-snapshot-home").get().asFile
    doFirst { home.mkdirs() }
    systemProperty("user.home", home.absolutePath)
    environment("XDG_CONFIG_HOME", File(home, "config").absolutePath)
    environment("XDG_STATE_HOME", File(home, "state").absolutePath)
    environment("XDG_DATA_HOME", File(home, "data").absolutePath)
    environment("LOCALAPPDATA", File(home, "local").absolutePath)
    environment("GSETTINGS_BACKEND", "memory")
}
registerDevTask("snap", "Snap", "Render the main window; pass arguments with -Pargs.").configure {
    if (providers.gradleProperty("snapOutput").isPresent) {
        setArgs(listOf(
            providers.gradleProperty("snapLaf").getOrElse("metal"),
            providers.gradleProperty("snapScale").getOrElse("1"),
            providers.gradleProperty("snapOutput").get()
        ))
    }
    systemProperty("awt.useSystemAAFontSettings", "on")
}
registerDevTask("verificaLeitura", "VerificaLeitura", "Check serialized diagrams and parts; pass arguments with -Pargs.").configure {
    systemProperty("java.awt.headless", "true")
}

// An explicit headless build takes precedence over ambient desktop variables.
val guiTests = !providers.gradleProperty("headless").isPresent && (
    providers.gradleProperty("gui").isPresent ||
    !System.getenv("DISPLAY").isNullOrBlank() || !System.getenv("WAYLAND_DISPLAY").isNullOrBlank())

tasks.test {
    dependsOn(tasks.jar)
    systemProperty("bundledHelp.jar", tasks.jar.get().archiveFile.get().asFile.absolutePath)
    systemProperty("serializedForm.classes", sourceSets.main.get().output.classesDirs.asPath)
    providers.gradleProperty("snapGridOutput").orNull?.let { systemProperty("brmodelo.snap.output", it) }
    if (providers.gradleProperty("seletorManual").isPresent) systemProperty("brmodelo.seletor.manual", "true")
    // CI sets -PexigirIsolamento where bubblewrap works: the real sandbox tests must run, not skip.
    if (providers.gradleProperty("exigirIsolamento").isPresent) systemProperty("brmodelo.exigirIsolamento", "true")
    // Tests run these scripts from the source tree.
    inputs.dir("packaging/launcher")
    useJUnitPlatform {
        if (!guiTests) excludeTags("gui")
    }
    systemProperty("java.awt.headless", (!guiTests).toString())
    providers.gradleProperty("oficialJar").orNull?.let { systemProperty("oficialJar", it) }
        ?: System.getProperty("oficialJar")?.let { systemProperty("oficialJar", it) }
    // Keep settings/autosave away from the developer's actual home, including XDG overrides.
    val home = layout.buildDirectory.dir("test-home").get().asFile
    doFirst { home.mkdirs() }
    systemProperty("user.home", home.absolutePath)
    environment("XDG_CONFIG_HOME", File(home, "config").absolutePath)
    environment("XDG_STATE_HOME", File(home, "state").absolutePath)
    environment("XDG_DATA_HOME", File(home, "data").absolutePath)
    environment("LOCALAPPDATA", File(home, "local").absolutePath)
    environment("GSETTINGS_BACKEND", "memory")
    testLogging { events("failed", "skipped"); showStandardStreams = false }
}

tasks.register<JavaExec>("snapshotSerializedForm") {
    group = "verification"
    description = "Intentionally rewrite the serialized-form golden for compatibility review."
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass = "brmodelo.SerializedForm"
    javaLauncher = javaToolchains.launcherFor(java.toolchain)
    systemProperty("serializedForm.classes", sourceSets.main.get().output.classesDirs.asPath)
    systemProperty("java.awt.headless", "true")
}

registerDevTask("geraFixtures", "GeraFixtures", "Generate invented test diagrams; requires a display.").configure {
    val home = layout.buildDirectory.dir("fixture-home").get().asFile
    doFirst { home.mkdirs() }
    systemProperty("user.home", home.absolutePath)
    environment("XDG_CONFIG_HOME", File(home, "config").absolutePath)
    environment("XDG_STATE_HOME", File(home, "state").absolutePath)
    environment("XDG_DATA_HOME", File(home, "data").absolutePath)
    environment("LOCALAPPDATA", File(home, "local").absolutePath)
    environment("GSETTINGS_BACKEND", "memory")
}

// Baselines are updated explicitly, never as a side effect of a normal test run.
tasks.register<JavaExec>("snapshotFixtures") {
    group = "verification"
    description = "Rewrite canonical fixture dumps for review after intentional fixture changes."
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass = "brmodelo.CanonicalDump"
    javaLauncher = javaToolchains.launcherFor(java.toolchain)
    systemProperty("java.awt.headless", "true")
}

// Uses the test dump helper; all conversion artifacts and application state stay under build/.
tasks.register<JavaExec>("verificaBrMj") {
    group = "verification"
    description = "Audit brM3/JSON/brM3 conversion, writing only to build/verificaBrMj; pass inputs with -Pargs."
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass = "brmodelo.VerificaBrMj"
    javaLauncher = javaToolchains.launcherFor(java.toolchain)
    args(Commandline.translateCommandline(providers.gradleProperty("args").getOrElse("")).toList())
    systemProperty("java.awt.headless", "true")
    providers.gradleProperty("oficialJar").orNull?.let { systemProperty("oficialJar", it) }
        ?: System.getProperty("oficialJar")?.let { systemProperty("oficialJar", it) }
    val saida = layout.buildDirectory.dir("verificaBrMj").get().asFile
    systemProperty("brmodelo.verifica.saida", saida.absolutePath)
    val home = File(saida, "audit-home")
    doFirst { home.mkdirs() }
    systemProperty("user.home", home.absolutePath)
    environment("XDG_CONFIG_HOME", File(home, "config").absolutePath)
    environment("XDG_STATE_HOME", File(home, "state").absolutePath)
    environment("XDG_DATA_HOME", File(home, "data").absolutePath)
    environment("LOCALAPPDATA", File(home, "local").absolutePath)
    environment("GSETTINGS_BACKEND", "memory")
}

registerDevTask("seletorDemo", "SeletorDemo", "Open all five file dialogs without saving files; -Pargs=portal|nativo|swing.").configure {
    val home = layout.buildDirectory.dir("seletor-home").get().asFile
    doFirst { home.mkdirs() }
    systemProperty("user.home", home.absolutePath)
    environment("XDG_CONFIG_HOME", File(home, "config").absolutePath)
    environment("XDG_STATE_HOME", File(home, "state").absolutePath)
    environment("XDG_DATA_HOME", File(home, "data").absolutePath)
    environment("LOCALAPPDATA", File(home, "local").absolutePath)
    environment("GSETTINGS_BACKEND", "memory")
}

// Package versions are independent of version fields serialized inside diagrams.
version = providers.gradleProperty("version").getOrElse("1.0.1")
apply(from = "packaging/packaging.gradle.kts")

// Help ships inside the jar, hence also inside every installer/portable package.
tasks.processResources {
    dependsOn("renderHelp")
    from(layout.buildDirectory.dir("generated/help")) { into("ajuda") }
    inputs.property("appVersion", provider { project.version.toString() })
    doLast {
        destinationDir.resolve("brmodelo-version.properties").writeText("version=${project.version}\n")
    }
}

// Markdown compiler is deliberately isolated from application/runtime dependencies.
val helpBuilder = sourceSets.create("helpBuilder") {
    java.setSrcDirs(listOf("tools/ajuda"))
}
dependencies {
    add(helpBuilder.implementationConfigurationName, libs.commonmark)
}
val renderHelp = tasks.register<JavaExec>("renderHelp") {
    group = "documentation"
    description = "Render and validate the ordered Markdown help as an offline site and a self-contained HTML file."
    classpath = helpBuilder.runtimeClasspath
    mainClass = "HelpSite"
    javaLauncher = javaToolchains.launcherFor(java.toolchain)
    inputs.dir("docs/ajuda")
    outputs.dir(layout.buildDirectory.dir("generated/help"))
    args("docs/ajuda", layout.buildDirectory.dir("generated/help").get().asFile.absolutePath)
}
sourceSets.test {
    compileClasspath += helpBuilder.output
    runtimeClasspath += helpBuilder.output + configurations[helpBuilder.runtimeClasspathConfigurationName]
}
tasks.test {
    dependsOn(helpBuilder.classesTaskName)
    systemProperty("help.sources", layout.projectDirectory.dir("docs/ajuda").asFile.absolutePath)
    val flatpakManifest = layout.projectDirectory.file("packaging/flatpak/io.github.kpagnussat.brModeloNG.yml")
    inputs.file(flatpakManifest)
    systemProperty("help.flatpakManifest", flatpakManifest.asFile.absolutePath)
}
