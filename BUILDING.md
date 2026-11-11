# Building brModelo NG

Install JDK 21. The Gradle 9.8.0 wrapper downloads Gradle on first use; dependencies
come from Maven Central. Toolchain downloads are disabled. Set `JAVA_HOME` to
your installed JDK 21.
For other local installations, configure `org.gradle.java.installations.paths`
in your user-level `~/.gradle/gradle.properties`, not in this repository.

```sh
export JAVA_HOME="/path/to/jdk21"
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew run
./gradlew build
./gradlew jar
java -jar build/libs/brModelo.jar
./gradlew snapDialogs -Pargs="flatescuro 1 /tmp/snaps t /path/to/conc.brM3 /path/to/logi.brM3"
```

Screenshot tasks require an X display. `dev/snap.sh <out-dir> [label]` renders a
matrix of themes and font scales through the Gradle `snap` task. Arguments to dev
tasks can contain quoted paths, for example `-Pargs='metal 1 "/tmp/my snap.png"'`.
`./gradlew verificaLeitura -Pargs="/path/to/samples"` checks serialized files.

Open the Gradle project in NetBeans or IntelliJ. Edit GUI forms in NetBeans;
`.form` files stay beside their `.java` files in `src/` and are excluded from the jar.

## Rendering on Linux

`util.Renderizacao` runs first in `main` and, on Linux, defaults
`sun.java2d.xrender` and `sun.java2d.pmoffscreen` to `false`. Swing then composes
each frame in Java memory and sends only the finished image to the X server.
With XRender and server-side pixmaps, every antialiased shape went to XWayland
as a separate mask that it composites on the CPU, which kept a core busy while
resizing. An explicit `-D` value on the command line still wins. On other
operating systems Java's defaults are left alone.

The editor keeps the paint path cheap: the grid draws only the lines inside the
clip, the page has a 1px outline and no shadow, and fixed-color interface SVG
icons are rasterized once per scale and theme (`util.IconeEmCache`). The palette
is a fixed-width column on the right edge, not a split pane, so it keeps its
width while the window is narrowed. Its button size follows the window height
(`util.Paleta`).

Java serialization walks the diagram graph recursively, and large, interlinked
diagrams outgrow the 1 MB stack of the Swing event thread. Opening, saving and
undo snapshots therefore run that walk on a thread with a deep stack
(`util.PilhaProfunda`). The file format does not change.

## Window decorations

Each OS gets its best frame by default. Windows uses FlatLaf's title bar with the
embedded menu; FlatLaf drives it through the native window manager, so snap
layouts, the system shadow and Windows 11's rounded corners keep working. Linux
and macOS use the system frame (macOS keeps its system menu bar). On Linux a
custom frame gets no compositor shadow, only a 1px outline in the theme's
`Component.borderColor`, and stays square: rounding it would need a translucent
window, which Java repaints transparent at each resize step (visible flicker).
Override the default before startup with `-Dbrmodelo.decoracoes=modernas` or
`-Dbrmodelo.decoracoes=nativas`.

Build the jar and launch with temporary state (Linux example):

```sh
./gradlew --dependency-verification strict jar
app_jar="$PWD/build/libs/brModelo.jar"
test_home=$(mktemp -d)
(cd "$test_home" && XDG_CONFIG_HOME="$test_home/config" \
  XDG_STATE_HOME="$test_home/state" XDG_DATA_HOME="$test_home/data" \
  java -Duser.home="$test_home" -Dbrmodelo.decoracoes=modernas -jar "$app_jar")
```

- Check light/dark themes and scale 1/2, including a live theme switch; title bar,
  menus, corners and dialogs should refresh together.
- Move the main window and dialogs; resize from all four edges and corners.
- Snap/tile against every edge, maximize by double-clicking, restore, minimize,
  and test the window menu (title-bar right-click and Alt+Space).
- Open modal and modeless dialogs; check focus, modality, closing, menu access
  and confirmation prompts for unsaved diagrams.
- If available, move between monitors with different scales; repeat resize,
  snapping and maximize/restore, checking bounds and corners.
- Relaunch without the flag and confirm the OS default (FlatLaf frame on Windows,
  system frame on Linux and macOS).

Reproduce tab/frame PNGs with isolated application state:

```sh
for theme in claro escuro; do
  for scale in 1 2; do
    ./gradlew --dependency-verification strict snapTabs \
      -Pargs="$theme $scale build/tabs modernas"
  done
done
```

This captures a single tab, hover, overflow, the full window and a dialog. Scale
uses FlatLaf's actual UI scaling, not only larger fonts.

## README screenshots

The README uses committed, invented fixtures. Render both themes with an X
display (prefix the Gradle command with `xvfb-run -a` on a headless machine):

```sh
for theme in flatclaro flatescuro; do
  ./gradlew --dependency-verification strict snapDialogs \
    -Pargs="$theme 1 build/readme-snaps/$theme readme test-resources/fixtures/conceitual.brM3 test-resources/fixtures/logico.brM3"
done
mkdir -p docs/img
cp build/readme-snaps/flatclaro/readme_main_conceitual.png docs/img/principal-claro.png
cp build/readme-snaps/flatescuro/readme_main_conceitual.png docs/img/principal-escuro.png
cp build/readme-snaps/flatescuro/readme_campos.png docs/img/editor-campos.png
cp build/readme-snaps/flatescuro/readme_status_erro.png docs/img/status-erro.png
```

The **Organize links** before/after pair comes from the algorithm's own GUI test,
which paints the diagram right before and after the action. The help page is a
headless browser screenshot of the generated help site:

```sh
./gradlew --dependency-verification strict test -Pgui \
  --tests brmodelo.OrganizarConexoesGuiTest -PsnapGridOutput=build/readme-snaps/organize
magick build/readme-snaps/organize/organize-before.png -crop 840x360+100+190 +repage -strip docs/img/organizar-antes.png
magick build/readme-snaps/organize/organize-after.png -crop 840x360+100+190 +repage -strip docs/img/organizar-depois.png

./gradlew --dependency-verification strict renderHelp
google-chrome --headless=new --hide-scrollbars --force-device-scale-factor=1 \
  --window-size=1200,720 --screenshot=docs/img/ajuda.png \
  "file://$PWD/build/generated/help/modelo-conceitual.html"
```

`snapDialogs` paints the Swing content without native title bars and borders.
The status PNG captures a deterministic example error through the application's
real logger and unread indicator; the renderer clears it before continuing.
Use a UI scale such as `1.5` instead of `1` to inspect larger layouts.
For a before/after matrix, run `snapDialogs` for `flatclaro` and `flatescuro`
at scales `1` and `2`, plus `sistema` at `1`. The renderer sets `flatlaf.uiScale` before setup, scaling fonts, icons and
geometry together. The main window grows with it. Scale-1 FlatLaf runs also produce
`*_live_claro.png` and `*_live_escuro.png` from the same editor instance, yielding
the EDT between theme changes so custom surfaces refresh. `sistema` uses GTK here.

PNG compression can be optimized losslessly after copying; retain readable text
and update AppStream dimensions if changing the main screenshot size.

The inspector type/state atlas uses the same invented fixtures and isolated application
home. It includes normal, hover, inactive selection and focused editing/action states,
actual menu/text/color editors, selected diagram objects, and asserts pixel samples for
the shared row/viewport/scrollbar/hints background. `tpNothing` produces no row;
read-only values have no editing state. Dialog actions retain their painted value cell.

```sh
for theme in flatclaro flatescuro; do
  for scale in 1 2; do
    ./gradlew --dependency-verification strict snapInspector \
      -Pargs="$theme $scale build/inspector-states test-resources/fixtures/conceitual.brM3 test-resources/fixtures/logico.brM3"
  done
done
```

## Automated tests

Tests use JUnit Jupiter 6.1.3 through the version catalog and stay in the flat
`test/` and `test-resources/` layout. `check` (and therefore `build`) runs `test`.
HTML reports are written to `build/reports/tests/test/index.html`; XML results
are in `build/test-results/test/`.

```sh
export JAVA_HOME="/path/to/jdk21"
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew test
./gradlew test -PoficialJar="/path/to/official/brModelo.jar"
# Equivalent system-property input:
./gradlew test -DoficialJar="/path/to/official/brModelo.jar"
# Headless: all GUI-tagged cases are excluded.
./gradlew test -Pheadless
# CI: a virtual X display runs all automated GUI cases.
xvfb-run -a ./gradlew build -Pgui
```

Editor JSON/autosave, print-preview, status-bar and chooser tests have the
`gui` tag because they use real Swing windows. They run when `DISPLAY` or
`WAYLAND_DISPLAY` is nonempty, or when `-Pgui` is supplied. Persistence (including
the serialized-form golden), security, JSON graph and
utility tests run headless. `-Pheadless` explicitly excludes the `gui` tag and
forces headless AWT, taking precedence over `-Pgui` and desktop variables.
`-Pgui` selects GUI tests; it does not create a display. Without `oficialJar`, the
six official-compatibility cases are skipped by a JUnit assumption. A supplied
but invalid jar fails the tests. The official jar is loaded with the platform
class loader as parent, so it cannot silently reuse the fork's classes. Both
serialization directions are checked, including the official diagram subclass,
element count and the reverse semantic dump.

The test task, fixture generator and dialog renderer set separate homes and absolute XDG config,
state and data directories under `build/`, keeping app settings/autosave out of
the developer's real home. `Pastas` has one package-private overload accepting an
environment lookup for deterministic directory tests. NG uses separate platform
state directories, including a writable Windows application-data directory.
The `.brM3` serialized model fields and identifiers retain compatibility with the
official application.

## Performance measurements

`dev/measure_performance.py` compiles once and runs fresh JVMs outside Gradle,
each with its own temporary home and XDG directories; inputs are only read and
their hashes are checked again at the end. `BRMODELO_BENCH_JAR` swaps NG for
another jar (such as the official one), using that jar's own look-and-feel setup
and main-window construction:

```sh
export JAVA_HOME="/path/to/jdk21"
BRMODELO_BENCH_JAR="/path/to/official/brModelo.jar" python3 dev/measure_performance.py build/perf/oficial --startup
python3 dev/measure_performance.py build/perf/ng --startup
BRMODELO_BENCH_JAR="/path/to/official/brModelo.jar" python3 dev/measure_performance.py build/perf/oficial-vp --viewport
python3 dev/measure_performance.py build/perf/ng-vp --viewport
```

`--startup` records the JVM uptime when the main window becomes visible
(7 cold starts). `--viewport` opens each fixture in the visible editor and times,
over 100 iterations after 10 warm-up ones, painting the visible area, a selection
click and a 40-pixel drag (3 runs). Results land in one CSV per run.

Measured for 1.0.0 on an Intel Core i7-1360P laptop, Fedora Linux, OpenJDK
21.0.12, against the official 3.3.2 jar (SHA-256 `c64c179b…ac513`). Startup is the
median of 7 runs; the other rows give the range of per-run medians across the six
fixtures:

| Measurement | Official 3.3.2 | NG 1.0.0 |
| --- | --- | --- |
| Startup to visible main window (median of 7) | 3.06 s | 0.90 s |
| Painting the visible area | 2.0 to 3.3 ms | 0.2 to 1.4 ms |
| Selection click | 0.04 to 1.3 ms | 0.05 to 0.7 ms |
| 40-pixel drag (NG includes snap to grid) | 0.13 to 0.63 ms | 0.27 to 1.4 ms |

The NG editor's visible area is about 10% smaller (about 760×513 against 864×516)
because of its layout, so the painting figures are indicative only. All
interaction times stay far below a 16 ms frame. The startup gain comes mostly
from creating the print dialog, with its native printer discovery, on first use.

## Invented fixtures and canonical dumps

`SerializedFormTest` scans every compiled main class implementing `Serializable`,
including nested/anonymous classes and Swing subclasses. It compares class names,
serial UIDs, ordered field names and JVM type signatures, and the full superclass
chain against `test-resources/serialized-form.txt`. Normal tests never rewrite it.
Regenerate only after an intentional, reviewed serialized-format change (which
may break compatibility with official brModelo files):

```sh
./gradlew snapshotSerializedForm
./gradlew test
```

Review the golden diff and run the official compatibility tests before accepting
such a change. Refactoring must preserve this golden to retain binary compatibility.

`dev/GeraFixtures.java` builds an invented Escola/Biblioteca Aurora example
through the palette/canvas command API (`ExternalRealiseComando`, shared with
`mousePressed`), lays it out with normal editor painting, and calls
`Diagrama.Salvar`. It requires a display. The committed `.brM3` resources cover
conceptual, logical, flow, activity, EAP and free diagrams. The conceptual file
contains entities, a relationship with different cardinalities, simple,
identifier and multivalued attributes, specialization, an associative entity and
text. The logical file contains three tables, PK/FK/unique flags and two linked
foreign-key constraints. Flow and activity show connected start/action/end
sequences; EAP shows a parent and child joined through its generated bar; free
shows two connected shapes. No coursework or external sample diagram is used.

Regenerate only for intentional fixture changes:

```sh
./gradlew geraFixtures
./gradlew snapshotFixtures
./gradlew snapDialogs -Pargs="flatclaro 1 /tmp/fx fixtures test-resources/fixtures/conceitual.brM3 test-resources/fixtures/logico.brM3 test-resources/fixtures/fluxo.brM3 test-resources/fixtures/atividade.brM3 test-resources/fixtures/eap.brM3 test-resources/fixtures/livre.brM3"
./gradlew test -PoficialJar="/path/to/official/brModelo.jar"
```

`snapDialogs` accepts additional diagram files after the conceptual and logical
inputs and renders each in the main editor. Inspect all six `main_*.png` images
in `/tmp/fx` for readable names, separated shapes and correctly attached lines
before accepting new baselines.

Review the binary fixtures together with their adjacent `.txt` dumps before
committing. UUIDs in regenerated binaries can differ. `snapshotFixtures` is an
explicit baseline writer using the test helper; normal tests never rewrite
expectations. The canonical dump preserves element/subelement order, classes,
text, document bounds, font/style, colors, flags, link points/endpoints, fields
and constraint references. It excludes editor state, UUIDs and lazy paint caches;
font names are recorded without environment-dependent resolved font families.
Round trips use the application's envelope and `Diagrama.SaveToStream`, and
validate both serialized layers with `LeitorSeguro`.

SVG tests parse and paint every shipped SVG and check every image-valued entry
in `Propriedades_pt_BR.properties`. `PAINTED_IN_DIAGRAM` in the SVG generator
explicitly lists the bitmap-only field, constraint and in-document handle icons;
new interface icons must have SVGs. `icone.svg` supplies the small interface icon. The window uses the full-color
application artwork from `packaging/brModelo.svg`, rasterized at build time.

## Optional JSON diagrams

See [docs/FORMATO-BRMJ.md](docs/FORMATO-BRMJ.md) for the version-1 graph format
and Save As behavior. The default remains `.brM3`. JSON fixture tests cover every
diagram type, determinism, metadata, conversion back to binary and rejection of
unknown classes/fields. Graph tests cover cycles, aliases, constructor behavior,
transient/final/hidden fields, exact numeric values and the supported AWT types.
GUI tests exercise JSON open/save and switching back to binary in the real editor.
The official compatibility test checks both the original fixture and its JSON
round trip against an isolated official jar.

A read-only conversion audit reuses the canonical test dump and writes all its
outputs and isolated application state under `build/verificaBrMj`:

```sh
./gradlew verificaBrMj -Pargs='"/path/to/files or directory"' \
  -PoficialJar="/path/to/official/brModelo.jar"
```

It reports OK/FAIL per `.brM3`, checks deterministic JSON (also after loading),
compares canonical dumps through JSON and back to binary, and optionally checks
the official jar in both directions. The report is
`build/verificaBrMj/conversoes/relatorio.txt`. The task never saves beside its inputs.

## Untrusted input

Files received from other people are treated as hostile. The README's
"Segurança" section describes the layers for users; this is where each lives.

- `util.LeitorSeguro` resolves Java serialization through a class allowlist and
  an `ObjectInputFilter` that refuses arrays and collection tables declared
  larger than the input (`SafeReaderTest`, which also guards that classes
  reachable from files keep no read callbacks).
- `util.LimitesEntrada` holds every input limit (bytes per format, image pixels)
  and refuses before reading (`LimitesEntradaTest`, `RecursosTest`, both probing
  in a child JVM with a small heap).
- `util.TrabalhadorImportacao` decodes `.brM3` and `.brMj` files in a headless
  child JVM (768 MB heap, 30 s deadline) that answers `.brMj`, read back with
  `FormatoBrMj`. Levels: isolated (bubblewrap on Linux, `flatpak-spawn --sandbox`
  in the Flatpak, see [Flatpak bundle](#flatpak-bundle)), plain child process,
  then in-process, each fallback logged as `WARN_IMPORT_WORKER_UNAVAILABLE`; a
  worker that started and failed means a refused file, never a fallback.
  Recovery, undo, the parts template (written by the app itself) and XML stay
  in-process. `TrabalhadorImportacaoTest` covers equal results for every fixture,
  deadline, memory exhaustion, oversized answers, cancellation and, where
  bubblewrap runs, the sandbox's missing files, network, display and bus.
- `controlador.AberturaEmSegundoPlano` runs that read off the EDT, shows a
  modal dialog with Cancel after 500 ms (interrupting the read kills the
  worker) and installs the diagram on the EDT unless the same file opened
  meanwhile (`AberturaEmSegundoPlanoGuiTest`). XML files still open
  synchronously.
- `XMLGenerate` parses without DTDs or external entities (`XmlSecurityTest`).
- `util.InterfaceSemHtml` disables Swing HTML rendering in every component except
  three fixed labels, and `util.RedeDesligada` refuses `http`, `https` and `ftp`
  URLs in the process (`HtmlInertTest` counts requests from a child JVM).
- `util.GravacaoAtomica` is the only way files are written: temporary file
  beside the target, sync, rename. `GravacaoAtomicaTest` scans `src/` for any
  other file writer.
- `diagramas.logico.NomeSql` quotes only names that could carry statements and
  restricts referential actions and separators (`DdlTest`, against a golden
  script generated by the code before the change).

Packaged network isolation (Linux launcher, `.msi` firewall rules, Flatpak
permissions) is described under
[Native packages](#native-packages-and-desktop-integration).

## Desktop file chooser

All five `Dialogos` file dialogs use `util.SeletorDeArquivos`. On Linux/BSD the
order is XDG Desktop Portal → AWT `FileDialog` → Swing `JFileChooser`; Windows and
macOS start with the system dialog through FlatLaf's `SystemFileChooser`
(`IFileDialog` on Windows, `NSOpenPanel`/`NSSavePanel` on macOS), which shows the
request's file-type filters in order, then fall back to AWT and Swing. AWT's
`FileDialog` has no file-type list (and on Windows ignores filename filters), so
with several save types it asks for the type first. A cancellation ends the selection; an absent interface,
D-Bus failure, response code 2 or malformed response tries the next backend.
The last successful folder is stored as `cfg.seletor.ultimaPasta` in `config.chc`.
Save dialogs retain binary `.brM3`, optional `.brMj` JSON and XML, PNG/BMP export,
and unrestricted export. The extension follows the selected filter, including
when a different suffix was typed. Overwrite confirmation checks the final path.

Force one backend for support or desktop testing:

```sh
java -Dbrmodelo.seletor=portal -jar build/libs/brModelo.jar
java -Dbrmodelo.seletor=sistema -jar build/libs/brModelo.jar
java -Dbrmodelo.seletor=nativo -jar build/libs/brModelo.jar
java -Dbrmodelo.seletor=swing -jar build/libs/brModelo.jar
```

A forced backend is strict: failures return `null` without trying another backend,
so portal support checks cannot accidentally succeed through AWT. Without a display,
selection returns `null` through the headless Swing path. Invalid backend names
raise an argument error.

The portal subscribes to the token-derived Request path before calling OpenFile or
SaveFile. It runs D-Bus calls and response waits on a worker; an application-modal
Swing wait dialog keeps the EDT painting while blocking app input. Its Cancel
button closes the portal request. It has no arbitrary browsing timeout; bus or
service loss triggers fallback. Globs match extension capitalization, and folder
paths use filesystem-encoded NUL-terminated byte arrays. Returned file URIs are
parsed as URIs, preserving spaces, Unicode, percent signs and literal plus signs.
See the [FileChooser protocol](https://flatpak.github.io/xdg-desktop-portal/docs/doc-org.freedesktop.portal.FileChooser.html).

The portal parent is an empty string. Java AWT provides no public X11 XID or
Wayland exported handle API; obtaining one here would require internal reflective
access or another native dependency. The portal window therefore may lack a
transient relationship, taskbar grouping or reliable positioning over the editor.
Swing still enforces application modality. No `--add-opens` is needed.

AWT has no portable filter picker, so this backend presents a filter choice before
its native dialog when several filters exist. AWT filename filtering is
platform-dependent (especially on Windows), and AWT owns the accept-button label.
The selected save format still controls extension completion. Swing retains its
usual filter picker. Neither fallback can promise the portal's desktop integration.

For a human desktop check, the dev task opens each of the five dialogs in order,
prints selected paths and never writes selected files. Application settings and
autosave are isolated under `build/seletor-home`; the host portal itself continues
to use the desktop's existing bookmarks.

```sh
./gradlew seletorDemo                       # strict portal by default
./gradlew seletorDemo -Pargs=nativo
./gradlew seletorDemo -Pargs=swing
# Equivalent opt-in @Tag("gui") human test, with isolated test-home:
./gradlew test -Pgui -PseletorManual --tests util.SeletorPortalManualTest
```

Check Recentes/Favoritos, pinned folders and search; select and cancel files,
change `.brM3` to `.brMj` or XML, and PNG to BMP. Verify overwrite confirmation
also when the target only exists after extension completion. The normal GUI tests
check that the portal worker leaves the EDT responsive with application modality,
that AWT really opens and cancels, and that Swing returns its selected JSON filter.
Pure tests cover wire options, URI decoding, extensions, backend order and mocked
portal error/absence/cancellation.

## CI and draft releases

`.github/workflows/ci.yml` builds on Linux, Windows and macOS with Temurin 21.
Linux runs every automated GUI test under Xvfb with `-Pgui`; Windows/macOS
explicitly use `-Pheadless`, excluding the `gui` tag. The human portal test needs
`-PseletorManual` and is skipped in CI. No session portal is required: automated
portal tests use mocked responses, and the application falls back when no bus is
available. CI does not supply the official jar, so its six compatibility cases
are explicitly reported as skipped by JUnit.

Each platform uploads its jar and HTML/XML test reports. Linux also uploads
`snapDialogs` PNGs for `flatclaro` and `flatescuro`, using only committed invented
fixtures. To reproduce those renders with a virtual display:

```sh
for theme in flatclaro flatescuro; do
  xvfb-run -a ./gradlew --dependency-verification strict snapDialogs \
    -Pargs="$theme 1 build/snapshots/$theme fixtures test-resources/fixtures/conceitual.brM3 test-resources/fixtures/logico.brM3 test-resources/fixtures/fluxo.brM3 test-resources/fixtures/atividade.brM3 test-resources/fixtures/eap.brM3 test-resources/fixtures/livre.brM3"
done
```

Tags of the form `vMAJOR.MINOR.PATCH` build the portable jar, Linux app-image
archive, DEB and RPM, Windows MSI and portable ZIP, macOS DMG and x86_64
Flatpak bundle.
`.github/workflows/release.yml` collects all outputs into a **draft** release.
The numeric tag sets Gradle `version` for the packages and staged AppStream
metadata. Nothing publishes the draft automatically. Reruns replace assets on a
draft only; they refuse to modify an already published release. Native installers
are unsigned. Windows and macOS builds run on their respective GitHub runners;
the macOS package follows the runner architecture.

## Dependency verification

`gradle/verification-metadata.xml` pins SHA-256 checksums of dependency artifacts
and their resolved POM/Gradle module metadata, including runtime and test
transitives. Gradle verifies these by default; CI also explicitly requests strict
verification. The `application` plugin is built into Gradle, so it has no separate
external plugin artifact. The wrapper distribution has its own SHA-256 in
`gradle/wrapper/gradle-wrapper.properties`; Actions are pinned to commit SHAs.

When intentionally changing dependencies or adding external plugins:

```sh
./gradlew --write-verification-metadata sha256 help build -Pheadless
# Resolve the dev renderer classpath too (requires a display).
xvfb-run -a ./gradlew --write-verification-metadata sha256 snapDialogs \
  -Pargs="flatclaro 1 build/snapshots/flatclaro fixtures test-resources/fixtures/conceitual.brM3 test-resources/fixtures/logico.brM3"
./gradlew --dependency-verification strict build -Pheadless
xvfb-run -a ./gradlew --dependency-verification strict build -Pgui
```

Checksum generation trusts the downloaded bytes; it is not an independent
provenance check. Review the metadata diff and check new hashes against upstream
published checksums before accepting it. Do not regenerate metadata merely to
silence a verification failure. Update `THIRD-PARTY.md` as needed; it lists the
Maven Central SHA-1 provenance hashes for the runtime dependencies. Gradle uses
the SHA-256 entries in the verification metadata for build verification.


## Native packages and desktop integration

The fork displays **brModelo NG**, uses app id
`io.github.kpagnussat.brModeloNG`, and installs with package/launcher name
`brmodelo-ng`. The About dialog credits the original brModelo and Carlos Henrique
Cândido. The original diagram versions and serialized form remain unchanged.
Both `.brM3` and optional `.brMj` files open as launcher arguments, including
multiple files and paths with spaces:

```sh
brmodelo-ng "/path/to/model.brM3" "/path/to/another model.brMj"
```

Gradle `version` defaults to `1.0.1`; override it with `-Pversion=1.0.2`.
AppStream and Flatpak metadata use the same generated version. Tagged release
builds pass `-PreleaseDate=YYYY-MM-DD` using the tag date; local packages are
marked as development snapshots dated at build time.
The About page reads the generated `brmodelo-version.properties`, using the
same version as the packages. `./gradlew renderHelp` renders `docs/ajuda/index.md`
and its ordered Markdown topics with build-only CommonMark. The deterministic
site is bundled under `ajuda/` in the jar and every package. F1 opens a copy in
the NG state directory in the system browser. Flatpak opens the single-file
edition (`ajuda-completa.html`: CSS and images inline, web links left clickable)
through the OpenURI portal, which exports one file. No external help file is
required.
Packaging accepts three numeric components. Every task resolves `jpackage`,
`jdeps` and `jlink` from the configured Java 21 toolchain, rather than the shell's
Java installation. `packaging/packaging.gradle.kts` contains the tasks.

`packaging/brModelo.svg` is the single source for the full-color ER app icon.
`generateAppIcons` uses the already verified JSVG dependency, with no native
rasterizer, to produce PNGs at 16, 24, 32, 48, 64, 128, 256 and 512 pixels, an ICO
and an ICNS in `build/generated/app-icons/`. These derived binaries are generated
at build time to avoid stale committed variants. `processResources` puts the PNGs
in the jar; the main window supplies all eight sizes to `setIconImages`.
`src/imagens/icone.png` remains available to legacy/diagram code.

`runtimeModules` runs `jdeps --multi-release 21 --ignore-missing-deps
--print-module-deps` on the fat jar. Fat-jar assembly removes dependency module
descriptors, since this application runs on the classpath. The ignore option
allows JSVG's compile-time-only Error Prone and JetBrains annotations, which are
absent at runtime. The task adds `java.desktop`, `java.prefs`, `java.logging` and
`jdk.unsupported`, and writes the resolved list to `build/packaging/modules.txt`.
`jlinkRuntime` creates the runtime in `build/packaging/runtime/`, stripping Java
debug attributes while retaining native symbols so no `objcopy` is needed.
The app-image and native installers include this runtime; users need no JDK.

Build on each target OS (jpackage does not cross-compile):

```sh
# Any OS: self-contained app directory under build/jpackage/app-image/.
./gradlew --dependency-verification strict jpackageImage
# Linux: needs dpkg-deb/fakeroot for DEB, rpmbuild for RPM.
./gradlew --dependency-verification strict jpackageDeb jpackageRpm
# Windows: JDK 21 requires WiX 3 (candle.exe and light.exe on PATH).
./gradlew --dependency-verification strict jpackageMsi
# macOS: creates the disk image using the host's packaging tools.
./gradlew --dependency-verification strict jpackageDmg
```

Windows and macOS display `brModelo NG`; their launcher is `brModelo NG.exe`
(Windows) or `brModelo NG.app` (macOS).
Output packages are under `build/jpackage/{deb,rpm,msi,dmg}/`. Tasks for other
operating systems are skipped. Linux installs under `/opt/brmodelo-ng`, exposes
`/usr/bin/brmodelo-ng`, and registers the app-id desktop entry, AppStream metadata,
SVG icon and shared MIME definitions. The generated DEB lifecycle scripts and RPM
spec preserve jpackage's lifecycle substitutions and install the integration
files with the fork's identity. RPM tracks the integration links in its file
list; DEB hooks create/remove them. The native file associations use
`packaging/brM3.properties` and `packaging/brMj.properties`, with generated
platform icons. Registering the shared types does not rename or change their
formats; desktop users choose their default application.

On Linux, `bin/brmodelo-ng` in the app-image (and so in the DEB, RPM and tar.gz)
is the script `packaging/launcher/brmodelo-ng`; jpackage's native launcher is
renamed `bin/brmodelo-ng-app`, with its `lib/app/brmodelo-ng-app.cfg`. The DEB and
RPM are built from that app-image and depend on `bubblewrap`. The script runs the
program as `bwrap --unshare-net --new-session --die-with-parent --dev-bind / /`:
the same files, display, session bus and later mounts, but a network namespace
holding only loopback. It sets `BRMODELO_ISOLAMENTO=rede`. When bwrap is missing
or cannot create the namespace (for example where unprivileged user namespaces
are restricted), the program starts as before, the script prints the reason on
stderr and the program's log shows `WARN_NETWORK_ISOLATION_UNAVAILABLE`.
`BRMODELO_ISOLAMENTO=desligado brmodelo-ng` skips the namespace. A browser started
from inside would stay offline, so `util.Navegador` (the single place links and
the help are opened) starts it from the desktop instead: web links through the
OpenURI portal (the connection stays open until the portal answers), the help
page with `systemd-run --user --wait --property=Type=exec xdg-open` (the portal
refuses `file:` URIs; a failing `xdg-open` moves on to the next way, one still
running after 15 s counts as opened, and `$` is doubled because systemd reads
`$$` as `$`). If both are unavailable it falls back to the usual launch
and logs `WARN_BROWSER_WITHOUT_NETWORK`. The namespace removes the network from
the program's own code; the session bus stays reachable, so it is a second layer
behind the import worker, which runs with neither. `LancadorLinuxTest` runs the
script against a stand-in program: isolated, bwrap failing, bwrap missing,
switched off, started through the `/usr/bin` symbolic link, exit status. The
probe and the real start use the same bwrap options. Tests of the real sandbox
skip where bubblewrap cannot run, except with `-PexigirIsolamento`, which CI and
release builds pass after enabling unprivileged user namespaces on the runner.

The Windows `.msi` installs for all users (elevated) and adds outbound block rules
to Windows Defender Firewall, all named `brModelo NG - sem rede`: one for
`brModelo NG.exe` (the launcher runs the JVM in its own process) and one each for
`runtime\bin\javaw.exe` and `runtime\bin\java.exe` (the import worker).
`preparePackaging` writes `main.wxs` to the jpackage resource directory: jpackage's
own template from the JDK's `jdk.jpackage` module with
`packaging/windows/firewall-actions.wxs.part` and
`packaging/windows/firewall-sequence.wxs.part` inserted, failing if a JDK update
moves the anchors. The actions are deferred `WixQuietExec64` calls of `netsh
advfirewall` (jpackage already links `WixUtilExtension`): every run first deletes
the rules by name, then adds them unless the product is being removed. Rollback
restores the state before the run: a failed fresh install leaves no rule, a
failed repair or uninstall gets its rules back. A failing `netsh` (firewall service stopped)
does not stop the installation. The portable `.zip` installs nothing and has no
rules. Links and the help open in the browser, a separate process the rules do
not cover. CI builds the `.msi` on every push so a WiX error shows before a tag.

NG stores state separately from the official application:
Linux uses `$XDG_CONFIG_HOME/brmodelo-ng`, `$XDG_STATE_HOME/brmodelo-ng` and
`$XDG_DATA_HOME/brmodelo-ng`; macOS uses
`~/Library/Application Support/brModelo NG`; Windows uses
`%LOCALAPPDATA%/brModelo NG` (or `~/AppData/Local/brModelo NG` when unset).
On first use, legacy files found in the working directory are copied into the NG
directories if no NG copy exists; originals are retained. Migration reads only
the working directory. Flatpak's XDG variables place NG state inside its
app-specific sandbox directories.

Validate and smoke-test without installing:

```sh
appstreamcli validate --no-net packaging/linux/io.github.kpagnussat.brModeloNG.metainfo.xml
desktop-file-validate packaging/linux/io.github.kpagnussat.brModeloNG.desktop
./gradlew --dependency-verification strict jpackageImage
bash packaging/smoke-test.sh
```

The smoke script requires a display (use `xvfb-run -a` in headless CI). It runs the
real app-image launcher under `timeout 15`, opens two committed invented fixtures
and checks the successful-open log entries. Its working directory, Java user home,
preferences and XDG directories are temporary; artifacts stay in the printed
`/tmp/brmodelo-ng-smoke.*` directory. Exit 124 means the GUI remains running until
the timeout. `LauncherTest` additionally verifies the window title, eight icon
sizes, multiple arguments and duplicate-path handling in the running editor.
AppStream's pedantic uppercase-id observation follows the chosen app id.
AppStream references the committed light-theme conceptual screenshot in `docs/img/`.

## Interface typeface

`util.FonteInterface` chooses the interface font before the FlatLaf theme is set up.
Windows and macOS use the bundled Adwaita Sans (`src/fontes/`, SIL OFL 1.1; see
THIRD-PARTY.md), so the program looks the same as on GNOME. Linux keeps the desktop's
own font as FlatLaf reads it; only when Java cannot find that family (it then reports
the logical `Dialog`) is the theme set up again with the bundled typeface. That happens
in the Flatpak on image-based systems such as Silverblue and Kinoite: the host's font
folders and the runtime's own share the zero timestamp of OSTree, so fontconfig
accepts the runtime's cache for `/run/host/fonts` and the host's fonts are not seen.
Diagram fonts are stored in the diagram and are not affected.

## Flatpak bundle

The manifest is `packaging/flatpak/io.github.kpagnussat.brModeloNG.yml`. It uses
Freedesktop Platform/SDK 26.08 and the matching OpenJDK 21 SDK extension. The
extension's `install.sh` bundles its JRE into `/app/jre`; the launcher runs the
fat jar with that JRE. `prepareFlatpak` stages strictly verified inputs outside
the builder sandbox, so no Maven or Gradle network access is needed during the
Flatpak module build. This is a release-bundle workflow, not a Flathub source-build
submission manifest.

On a machine with flatpak-builder and those runtimes already installed:

```sh
./gradlew --dependency-verification strict prepareFlatpak
python3 -c 'import yaml; yaml.safe_load(open("packaging/flatpak/io.github.kpagnussat.brModeloNG.yml"))'
flatpak-builder --user --force-clean --repo=build/flatpak-repo \
  build/flatpak-build packaging/flatpak/io.github.kpagnussat.brModeloNG.yml
flatpak build-bundle build/flatpak-repo build/brmodelo-ng.flatpak \
  io.github.kpagnussat.brModeloNG --runtime-repo=https://flathub.org/repo/flathub.flatpakrepo
```

Permissions are X11 (also through XWayland), IPC and DRI. There is no network
permission: the app makes no network calls, and the help needs none. There is no
file system permission either: the launcher forces the existing `PortalDeArquivos`
backend, and each file chosen there or opened from the file manager (the desktop
entry's `%F` becomes `--file-forwarding`) arrives as a document-portal path under
`/run/user/<uid>/doc/`, which stays valid for the recent-files list. Saving over
such a file still writes a temporary file beside it and renames it over the
original, which the document portal supports (new inode, same permissions, no
temporary left). Configuration, autosave, parts and the help live in the app's
own data directories.
The import worker runs as `flatpak-spawn --sandbox --no-network --watch-bus
--clear-env --directory=/`, plus `--expose-pids` when the Flatpak portal reports
support for it (it does not with a setuid bwrap). Its JVM starts under
`/usr/bin/env -i`, because the portal gives the new sandbox the session's
environment even with `--clear-env`. At the deadline the program kills the worker
itself, found by a per-run `-Dbrmodelo.trabalhador=` argument: the portal sends
signals to its bwrap, which does not pass them on, so killing `flatpak-spawn`
alone would leave the worker running. Finding it needs `--expose-pids`; without
that support the worker stays until its own clock, set to the same deadline,
halts it.
Plain Swing `JFileChooser` has no portal support and is intentionally excluded as
a fallback inside this package. Linux Java 21 Swing requires X11/XWayland in
Wayland sessions. The release workflow builds the bundle with
`flatpak/flatpak-github-actions/flatpak-builder`, pinned to the peeled commit of
v6, and attaches it with the native packages.

The release workflow builds native formats on their target operating systems and
the Flatpak bundle in its SDK container. The smoke test covers the Linux app-image;
it does not install or exercise the native installers or the Flatpak bundle.
