# Third-party dependencies

Resolved from Maven Central through the Gradle version catalog and merged into
`build/libs/brModelo.jar`, which runs with Java 21 and plain `java -jar`. Versions
are pinned in `gradle/libs.versions.toml`; Gradle verifies artifact and metadata
SHA-256 checksums using `gradle/verification-metadata.xml`. The Maven Central
SHA-1 provenance hashes below are provided for reference.

| Jar | Version | License | SHA-1 (as published on Maven Central) |
|---|---|---|---|
| [FlatLaf](https://www.formdev.com/flatlaf/) `com.formdev:flatlaf` | 3.7.2 | Apache-2.0 | `fcf9f3f276b11c7f5fc525838073ddcd80abda6a` |
| FlatLaf Extras `com.formdev:flatlaf-extras` (SVG icons) | 3.7.2 | Apache-2.0 | `d5e49b24c5310fc12857fc85c5a4833306f1be7d` |
| [JSVG](https://github.com/weisJ/jsvg) `com.github.weisj:jsvg` (SVG renderer used by FlatLaf Extras) | 2.1.0 | MIT | `c1a403f318de302fa81e4403fe8fb4bd6056a41f` |

JSVG is pinned to 2.1.0 because that is the version flatlaf-extras 3.7.2 declares.
The three UI dependency jars above target Java 8; this fork builds and runs on Java 21.
The D-Bus dependency requirements are described below.

Generic action icons in `src/imagens/svg/` come from Lucide 1.52.0 (ISC license).
See [LICENSE-lucide.txt](src/imagens/svg/LICENSE-lucide.txt). Diagram symbols in
that directory are drawn by the in-repository generator `dev/icones/gerar_svg.py`.
The full-color application icon is `packaging/brModelo.svg`.

The interface typeface `src/fontes/AdwaitaSans-Regular.ttf` is Adwaita Sans, GNOME's
interface font (derived from Inter), from adwaita-fonts 50.0, under the SIL Open Font
License 1.1: see [LICENSE-adwaita-fonts.txt](src/fontes/LICENSE-adwaita-fonts.txt). It was
taken from `https://download.gnome.org/sources/adwaita-fonts/50/adwaita-fonts-50.0.tar.xz`
(SHA-256 `4c927fbfeec1c503801ba510c2c94e0054c82c522cf7ba0d3be5d4d41fcf5c86`, as published
beside it); the file's SHA-256 is `8381c33b9a44f066f2b99dba3d416a2342891e28c956a35dfd8d16ee2987e6d4`,
which `FonteInterfaceTest` checks.

The optional `.brMj` format adds no dependency. `util.Json` is a small in-repository
syntax parser/writer with strict JSON validation, exact decimal parsing and tests;
`util.FormatoBrMj` owns the explicit allowlisted graph mapping. No external JSON
framework resolves classes or constructs model objects.

## Desktop file chooser

| Jar | Version | License | SHA-1 (Maven Central) |
|---|---|---|---|
| [dbus-java core](https://github.com/hypfvieh/dbus-java) `com.github.hypfvieh:dbus-java-core` | 5.2.2 | MIT | `8b7c3a4bba6199704ea42c4c7e4421167362e76c` |
| dbus-java JDK Unix socket transport `com.github.hypfvieh:dbus-java-transport-native-unixsocket` | 5.2.2 | MIT | `b088bbcf244579cf5dc5e810ee16c5d260190e67` |
| [SLF4J API](https://www.slf4j.org/license.html) `org.slf4j:slf4j-api` (transitive) | 2.0.17 | MIT | `d9e58ac9c7779ba3bf8142aff6c830617a7fe60f` |
| SLF4J no-op provider `org.slf4j:slf4j-nop` | 2.0.17 | MIT | `b41773047e10359aa409e7197c7624aeabfd377c` |

The core implements the XDG Desktop Portal D-Bus protocol. Its JDK transport uses
`UnixDomainSocketAddress` and `SocketChannel` (JDK 16+); the core requires JDK 17+.
All four D-Bus/logging jars contain only Java/resources, with no JNI, JNA, JNR or bundled
native libraries. SLF4J is the core's logging API; the no-op provider avoids a
missing-provider message at startup. Backend failures use the app's JDK logger.
The D-Bus/logging runtime jars total approximately 458 KiB. FileChooser exchanges paths
and URIs, so file descriptor passing is unnecessary. Abstract Unix sockets are
unsupported by this transport; those session buses fall back to AWT/Swing.

The fat jar retains the transport and SLF4J `META-INF/services` provider entries;
there is one provider for each service, so no service-file merging is required.
The dependencies have pinned versions in `gradle/libs.versions.toml`.
Their artifacts and transitive metadata are pinned with SHA-256 in
`gradle/verification-metadata.xml`; see `BUILDING.md` for reviewed updates.
FlatLaf also ships its own platform-native resources, which are retained in the fat jar.

## Appearance themes

`com.formdev:flatlaf-intellij-themes:3.7.2` uses the same version as FlatLaf core
(Apache-2.0). Maven Central JAR SHA-1: `6be9b826459e9075f669ba3752fc68b76f4a3cf0`.
The curated picker exposes these themes; stable configuration IDs are in parentheses.
The upstream theme pack's theme-specific license and copyright files under
`com/formdev/flatlaf/intellijthemes/themes/` are retained in the application JAR,
including the `material-theme-ui-lite/` subdirectory.

| Theme | License | Bundled license / source |
|---|---|---|
| FlatLaf Light (`claro`) | Apache-2.0 | FlatLaf core |
| FlatLaf Dark (`escuro`) | Apache-2.0 | FlatLaf core |
| macOS Light (`macos-claro`) | Apache-2.0 | FlatLaf core |
| macOS Dark (`macos-escuro`) | Apache-2.0 | FlatLaf core |
| IntelliJ (`intellij`) | Apache-2.0 | FlatLaf core |
| Darcula (`darcula`) | Apache-2.0 | FlatLaf core |
| Arc (`arc`) | MIT | `arc-themes.LICENSE.txt` |
| Arc Dark (`arc-escuro`) | MIT | `arc-themes.LICENSE.txt` |
| One Dark (`one-dark`) | MIT | `one_dark.LICENSE.txt` |
| Nord (`nord`) | MIT | `nord.LICENSE.txt` |
| Dracula (`dracula`) | MIT | `Dracula.LICENSE.txt` |
| Solarized Light (`solarized-claro`) | Unlicense | `Solarized.LICENSE.txt` |
| Solarized Dark (`solarized-escuro`) | Unlicense | `Solarized.LICENSE.txt` |
| GitHub Light (`github-claro`) | MIT | `material-theme-ui-lite/Material Theme UI Lite.LICENSE.txt` |
| GitHub Dark (`github-escuro`) | MIT | `material-theme-ui-lite/Material Theme UI Lite.LICENSE.txt` |
| Material Lighter (`material-claro`) | MIT | `material-theme-ui-lite/Material Theme UI Lite.LICENSE.txt` |
| Material Darker (`material-escuro`) | MIT | `material-theme-ui-lite/Material Theme UI Lite.LICENSE.txt` |
| Gruvbox Dark (`gruvbox-escuro`) | MIT | `gruvbox_theme.LICENSE.txt` |

License provenance: [theme pack at the pinned 3.7.2 tag](https://github.com/JFormDesigner/FlatLaf/tree/3.7.2/flatlaf-intellij-themes/src/main/resources/com/formdev/flatlaf/intellijthemes/themes).
All listed licenses permit redistribution. Solarized here is the theme pack's
Unlicense adaptation; GitHub uses the MIT Material Theme UI Lite adaptation.

## Markdown help (build only)

`org.commonmark:commonmark:0.24.0` ([upstream pinned release](https://github.com/commonmark/commonmark-java/tree/commonmark-parent-0.24.0))
parses the help sources during `renderHelp`. It is isolated in the `helpBuilder`
source set, absent from the application runtime and distributed fat jar.
License: BSD-2-Clause; [upstream license](https://github.com/commonmark/commonmark-java/blob/commonmark-parent-0.24.0/LICENSE.txt).
The version catalog and SHA-256 verification metadata pin its artifact and POMs.
The application bundles only the generated HTML/CSS and project screenshots.
