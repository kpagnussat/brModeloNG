import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.jvm.toolchain.JavaToolchainService
import java.util.zip.ZipFile

val appId = "io.github.kpagnussat.brModeloNG"
val packageName = "brmodelo-ng"
val os = System.getProperty("os.name").lowercase()
val windows = os.contains("win")
val mac = os.contains("mac")
val linux = os.contains("linux")
val javaExtension = extensions.getByType<JavaPluginExtension>()
val toolchains = extensions.getByType<JavaToolchainService>()
val packagingJava = toolchains.launcherFor(javaExtension.toolchain)
val jdk = packagingJava.map { it.metadata.installationPath }
fun jdkTool(name: String) = jdk.get().file("bin/$name" + if (windows) ".exe" else "").asFile.absolutePath
val sets = extensions.getByType<SourceSetContainer>()
val iconTools = sets.create("iconTools") { java.setSrcDirs(listOf("packaging/tools")) }
// Reuse the already verified rasterizer; no additional build dependency is downloaded.
dependencies.add(iconTools.implementationConfigurationName, extensions.getByType<VersionCatalogsExtension>().named("libs").findLibrary("jsvg").get())
val icons = layout.buildDirectory.dir("generated/app-icons")
val generateAppIcons = tasks.register<JavaExec>("generateAppIcons") {
    group = "build"
    description = "Rasterize the application SVG to PNG, ICO and ICNS with JSVG."
    classpath = iconTools.runtimeClasspath
    mainClass = "GenerateIcons"
    javaLauncher = toolchains.launcherFor(javaExtension.toolchain)
    systemProperty("java.awt.headless", "true")
    args(file("packaging/brModelo.svg"), icons.get().asFile)
    inputs.file("packaging/brModelo.svg")
    outputs.dir(icons)
}
tasks.named<ProcessResources>("processResources") {
    dependsOn(generateAppIcons)
    from(icons) { include("*.png"); into("imagens/app") }
}
val jarTask = tasks.named<Jar>("jar")
val inputDir = layout.buildDirectory.dir("packaging/input")
val resourceDir = layout.buildDirectory.dir("packaging/resources")
val assocDir = layout.buildDirectory.dir("packaging/associations")
// Development snapshots carry their build date; tagged releases supply the tag date.
val releaseDate = providers.gradleProperty("releaseDate")
val metadataDate = releaseDate.orElse(provider { java.time.LocalDate.now(java.time.ZoneOffset.UTC).toString() })
fun jpackageResource(name: String) = ZipFile(jdk.get().file("jmods/jdk.jpackage.jmod").asFile).use { zip ->
    zip.getInputStream(zip.getEntry("classes/jdk/jpackage/internal/resources/$name")).bufferedReader().use { it.readText() }
}
// jpackage's own main.wxs with the outbound firewall rules of packaging/windows inserted;
// the build fails if a JDK update moves the anchors.
fun mainWxsWithFirewall(template: String): String {
    val root = "    <!-- Standard required root -->"
    val sequenceEnd = "    </InstallExecuteSequence>"
    for (anchor in listOf(root, sequenceEnd)) {
        require(template.split(anchor).size == 2) { "jpackage main.wxs changed: \"${anchor.trim()}\" must appear once" }
    }
    return template
        .replace(root, file("packaging/windows/firewall-actions.wxs.part").readText() + root)
        .replace(sequenceEnd, file("packaging/windows/firewall-sequence.wxs.part").readText() + sequenceEnd)
}
val preparePackaging = tasks.register("preparePackaging") {
    dependsOn(jarTask, generateAppIcons)
    inputs.files(jarTask.flatMap { it.archiveFile }, fileTree("packaging/linux"), file("LICENSE"),
        file("packaging/brM3.properties"), file("packaging/brMj.properties"), file("packaging/brModelo.svg"),
        fileTree("packaging/windows"))
    inputs.property("packageVersion", provider { project.version.toString() })
    inputs.property("metadataDate", metadataDate)
    inputs.property("stableRelease", releaseDate.isPresent)
    inputs.property("jdkRuntimeVersion", packagingJava.map { it.metadata.javaRuntimeVersion })
    outputs.dirs(inputDir, resourceDir, assocDir)
    doLast {
        require(project.version.toString().matches(Regex("[0-9]+\\.[0-9]+\\.[0-9]+"))) {
            "jpackage requires a numeric version (use -Pversion=MAJOR.MINOR.PATCH)."
        }
        delete(inputDir, resourceDir, assocDir)
        copy { from(jarTask.flatMap { it.archiveFile }); into(inputDir) }
        copy { from("LICENSE"); into(inputDir) }
        copy { from("packaging/linux"); into(inputDir.get().dir("linux")) }
        copy { from("packaging/brModelo.svg"); into(inputDir.get().dir("linux")) }
        // Each release advertises the same version as the package, without changing model fields.
        val metadata = inputDir.get().file("linux/$appId.metainfo.xml").asFile
        metadata.writeText(metadata.readText().replace("@APP_VERSION@", project.version.toString())
            .replace("@RELEASE_DATE@", metadataDate.get())
            .replace("@RELEASE_TYPE@", if (releaseDate.isPresent) "stable" else "development"))
        val icon = icons.get().file(if (windows) "brModelo.ico" else if (mac) "brModelo.icns" else "brModelo-256.png").asFile
        assocDir.get().asFile.mkdirs()
        for (extension in listOf("brM3", "brMj")) {
            assocDir.get().file("$extension.properties").asFile.writeText(
                file("packaging/$extension.properties").readText() + "icon=" + icon.absolutePath.replace("\\", "/") + "\n")
        }
        resourceDir.get().asFile.mkdirs()
        if (windows) resourceDir.get().file("main.wxs").asFile.writeText(mainWxsWithFirewall(jpackageResource("main.wxs")))
        if (linux) {
            // jpackage substitutes its launcher/icon paths in this desktop template.
            resourceDir.get().file("$packageName.desktop").asFile.writeText(
                file("packaging/linux/$appId.desktop").readText()
                    .replace("Exec=brmodelo-ng %F", "Exec=APPLICATION_LAUNCHER %F")
                    .replace("Icon=$appId", "Icon=APPLICATION_ICON"))
            val content = "/opt/$packageName/lib/app/linux"
            val share = mapOf(
                "applications/$appId.desktop" to "$appId.desktop",
                "metainfo/$appId.metainfo.xml" to "$appId.metainfo.xml",
                "mime/packages/$appId.xml" to "$appId.xml",
                "icons/hicolor/scalable/apps/$appId.svg" to "brModelo.svg")
            val desktop = inputDir.get().file("linux/$appId.desktop").asFile
            desktop.writeText(desktop.readText().replace("Exec=brmodelo-ng", "Exec=/opt/$packageName/bin/$packageName"))
            val install = buildString {
                append("mkdir -p /usr/bin\nln -sf /opt/$packageName/bin/$packageName /usr/bin/$packageName\n")
                for ((target, source) in share) {
                    append("mkdir -p /usr/share/${target.substringBeforeLast('/')}\n")
                    append("ln -sf $content/$source /usr/share/$target\n")
                }
                append("update-desktop-database /usr/share/applications 2>/dev/null || true\n")
                append("update-mime-database /usr/share/mime 2>/dev/null || true\n")
                append("gtk-update-icon-cache -f -t /usr/share/icons/hicolor 2>/dev/null || true\n")
            }
            val remove = "rm -f /usr/bin/$packageName " + share.keys.joinToString(" ") { "/usr/share/$it" } +
                "\nupdate-desktop-database /usr/share/applications 2>/dev/null || true\nupdate-mime-database /usr/share/mime 2>/dev/null || true\ngtk-update-icon-cache -f -t /usr/share/icons/hicolor 2>/dev/null || true\n"
            // Keep stock lifecycle hooks, but our app-id desktop/MIME files replace xdg-desktop-menu registration.
            run {
                fun template(name: String) = jpackageResource("template.$name")
                resourceDir.get().file("postinst").asFile.writeText(
                    template("postinst").replace("DESKTOP_COMMANDS_INSTALL", install))
                resourceDir.get().file("prerm").asFile.writeText(
                    template("prerm").replace("DESKTOP_COMMANDS_UNINSTALL", remove))
                // RPM owns the integration files: create them in buildroot before the stock file-list scan.
                val rpmInstall = buildString {
                    append("mkdir -p %{buildroot}/usr/bin\nln -s /opt/$packageName/bin/$packageName %{buildroot}/usr/bin/$packageName\n")
                    for ((target, source) in share) {
                        append("mkdir -p %{buildroot}/usr/share/${target.substringBeforeLast('/')}\n")
                        append("ln -s $content/$source %{buildroot}/usr/share/$target\n")
                    }
                }
                resourceDir.get().file("$packageName.spec").asFile.writeText(
                    template("spec")
                        .replace("DESKTOP_COMMANDS_INSTALL", "update-desktop-database /usr/share/applications 2>/dev/null || true\nupdate-mime-database /usr/share/mime 2>/dev/null || true\ngtk-update-icon-cache -f -t /usr/share/icons/hicolor 2>/dev/null || true")
                        .replace("DESKTOP_COMMANDS_UNINSTALL", "")
                        .replace("%clean", "%postun\nupdate-desktop-database /usr/share/applications 2>/dev/null || true\nupdate-mime-database /usr/share/mime 2>/dev/null || true\ngtk-update-icon-cache -f -t /usr/share/icons/hicolor 2>/dev/null || true\n\n%clean")
                        .replace("(cd %{buildroot} && find . -path", rpmInstall + "(cd %{buildroot} && find . -path"))
            }
        }
    }
}
val moduleFile = layout.buildDirectory.file("packaging/modules.txt")
val runtimeDir = layout.buildDirectory.dir("packaging/runtime")
val runtimeModules = tasks.register<Exec>("runtimeModules") {
    dependsOn(jarTask)
    inputs.file(jarTask.flatMap { it.archiveFile })
    outputs.file(moduleFile)
    doFirst {
        moduleFile.get().asFile.parentFile.mkdirs()
        val result = java.io.ByteArrayOutputStream()
        standardOutput = result
        // JSVG references compile-time-only Error Prone/JetBrains annotations.
        commandLine(jdkTool("jdeps"), "--multi-release", "21", "--ignore-missing-deps", "--print-module-deps", jarTask.get().archiveFile.get().asFile)
    }
    doLast {
        val detected = standardOutput.toString().trim().split(",")
        val modules = (detected + listOf("java.desktop", "java.prefs", "java.logging", "jdk.unsupported"))
            .distinct().sorted().joinToString(",")
        moduleFile.get().asFile.writeText(modules + "\n")
        logger.lifecycle("jlinked runtime modules: $modules")
    }
}
val jlinkRuntime = tasks.register<Exec>("jlinkRuntime") {
    dependsOn(runtimeModules)
    inputs.file(moduleFile)
    inputs.property("jdkRuntimeVersion", packagingJava.map { it.metadata.javaRuntimeVersion })
    outputs.dir(runtimeDir)
    doFirst {
        delete(runtimeDir)
        commandLine(jdkTool("jlink"), "--add-modules", moduleFile.get().asFile.readText().trim(),
            "--strip-java-debug-attributes", "--no-header-files", "--no-man-pages", "--output", runtimeDir.get().asFile)
    }
}
val launcherScript = file("packaging/launcher/$packageName")
val linuxImage = layout.buildDirectory.dir("jpackage/app-image/$packageName")
fun registerPackage(taskName: String, type: String, supported: Boolean) = tasks.register<Exec>(taskName) {
    group = "distribution"
    description = "Build the brModelo NG $type with the Java 21 toolchain."
    dependsOn(preparePackaging, jlinkRuntime)
    onlyIf("$type is only supported on its target OS") { supported }
    val destination = layout.buildDirectory.dir("jpackage/$type")
    // Linux installers package the finished app-image, so they ship its launcher script.
    val fromImage = linux && type != "app-image"
    // An input, not only an order: a change to the image alone rebuilds the installers.
    if (fromImage) inputs.files(tasks.named("jpackageImage"))
    inputs.files(inputDir, runtimeDir, resourceDir, assocDir, icons)
    if (linux) inputs.file(launcherScript)
    inputs.property("version", provider { project.version.toString() })
    outputs.dir(destination)
    doFirst {
        delete(destination)
        destination.get().asFile.mkdirs()
        val iconName = if (windows) "brModelo.ico" else if (mac) "brModelo.icns" else "brModelo-256.png"
        val command = mutableListOf(jdkTool("jpackage"), "--type", type, "--name", if (linux) packageName else "brModelo NG",
            "--app-version", project.version.toString(), "--vendor", "brModelo NG contributors",
            "--description", "brModelo NG — entity-relationship and database diagram editor")
        command += if (fromImage) listOf("--app-image", linuxImage.get().asFile.absolutePath)
            else listOf("--input", inputDir.get().asFile.absolutePath, "--main-jar", "brModelo.jar",
                "--main-class", "principal.Aplicacao", "--runtime-image", runtimeDir.get().asFile.absolutePath)
        command += listOf("--icon", icons.get().file(iconName).asFile.absolutePath, "--dest", destination.get().asFile.absolutePath,
            "--resource-dir", resourceDir.get().asFile.absolutePath, "--verbose")
        if (mac) command += listOf("--mac-package-name", "brModelo NG", "--mac-package-identifier", appId)
        if (type != "app-image") {
            command += listOf("--file-associations", assocDir.get().file("brM3.properties").asFile.absolutePath,
                "--file-associations", assocDir.get().file("brMj.properties").asFile.absolutePath,
                "--license-file", file("LICENSE").absolutePath,
                "--about-url", "https://github.com/kpagnussat/brModeloNG")
            if (linux) command += listOf("--linux-package-name", packageName, "--install-dir", "/opt",
                "--linux-shortcut", "--linux-menu-group", "Education;Development;Office",
                // The launcher script runs the program without network through bubblewrap.
                "--linux-package-deps", "bubblewrap")
            if (type == "rpm") command += listOf("--linux-rpm-license-type", "GPL-3.0-or-later")
            if (windows) command += listOf("--win-menu", "--win-shortcut", "--win-menu-group", "brModelo NG",
                "--win-upgrade-uuid", "c3b64dbd-3cd0-4b5e-af83-a4843395c436", "--install-dir", "brModelo NG")
        }
        logger.lifecycle("jpackage command: ${command.joinToString(" ")}")
        commandLine(command)
    }
    if (linux && type == "app-image") doLast {
        // jpackage's native launcher finds lib/app/<its own name>.cfg; the script takes its
        // place in bin/, so desktop entries, /usr/bin and the tar.gz all start the script.
        val image = linuxImage.get().asFile
        image.resolve("bin/$packageName").renameTo(image.resolve("bin/$packageName-app"))
        image.resolve("lib/app/$packageName.cfg").renameTo(image.resolve("lib/app/$packageName-app.cfg"))
        launcherScript.copyTo(image.resolve("bin/$packageName"))
        image.resolve("bin/$packageName").setExecutable(true, false)
        check(image.resolve("bin/$packageName-app").canExecute() && image.resolve("lib/app/$packageName-app.cfg").isFile) {
            "jpackage launcher layout changed: bin/$packageName and lib/app/$packageName.cfg were expected"
        }
    }
}
registerPackage("jpackageImage", "app-image", true)
registerPackage("jpackageDeb", "deb", linux)
registerPackage("jpackageRpm", "rpm", linux)
registerPackage("jpackageMsi", "msi", windows)
registerPackage("jpackageDmg", "dmg", mac)

tasks.register<Sync>("prepareFlatpak") {
    group = "distribution"
    description = "Stage the verified fat jar and desktop assets for flatpak-builder."
    dependsOn(jarTask, preparePackaging)
    into(layout.buildDirectory.dir("flatpak-input"))
    from(jarTask.flatMap { it.archiveFile })
    from("packaging/linux") { exclude("*.metainfo.xml") }
    from(inputDir.map { it.file("linux/$appId.metainfo.xml") })
    from("packaging/brModelo.svg", "packaging/flatpak/brmodelo-ng", "LICENSE")
}
