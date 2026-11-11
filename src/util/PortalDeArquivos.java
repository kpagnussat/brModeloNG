package util;

import java.awt.Component;
import java.io.File;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.annotations.Position;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBus;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.matchrules.DBusMatchRuleBuilder;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;

/** XDG FileChooser protocol; all bus calls and response waits run outside the EDT. */
public final class PortalDeArquivos implements SeletorDeArquivos.Backend {
    static final String DESTINO = "org.freedesktop.portal.Desktop";
    static final String CAMINHO = "/org/freedesktop/portal/desktop";
    static final String INTERFACE = "org.freedesktop.portal.FileChooser";

    @DBusInterfaceName(INTERFACE)
    public interface FileChooser extends DBusInterface {
        DBusPath OpenFile(String parent, String title, Map<String, Variant<?>> options);
        DBusPath SaveFile(String parent, String title, Map<String, Variant<?>> options);
    }

    @DBusInterfaceName("org.freedesktop.portal.Request")
    public interface Request extends DBusInterface {
        void Close();
        final class Response extends DBusSignal {
            public final UInt32 response;
            public final Map<String, Variant<?>> results;
            public Response(String path, UInt32 response, Map<String, Variant<?>> results) throws DBusException {
                super(path, response, results);
                this.response = response;
                this.results = results;
            }
        }
    }

    public static final class Pattern extends Struct {
        @Position(0) public final UInt32 kind;
        @Position(1) public final String pattern;
        public Pattern(UInt32 kind, String pattern) { this.kind = kind; this.pattern = pattern; }
    }
    public static final class Filter extends Struct {
        @Position(0) public final String name;
        @Position(1) public final List<Pattern> patterns;
        public Filter(String name, List<Pattern> patterns) { this.name = name; this.patterns = patterns; }
    }

    static String glob(String extensao) {
        StringBuilder pattern = new StringBuilder("*.");
        for (char c : extensao.toCharArray()) {
            if (Character.isLetter(c)) pattern.append('[').append(Character.toLowerCase(c))
                    .append(Character.toUpperCase(c)).append(']');
            else pattern.append(c);
        }
        return pattern.toString();
    }

    static Filter filtro(SeletorDeArquivos.Filtro filtro) {
        List<Pattern> patterns = filtro.extensoes().isEmpty() ? List.of(new Pattern(new UInt32(0), "*"))
                : filtro.extensoes().stream().map(ext -> new Pattern(new UInt32(0), glob(ext))).toList();
        return new Filter(filtro.nome(), patterns);
    }

    static Map<String, Variant<?>> opcoes(SeletorDeArquivos.Pedido pedido, String token) {
        Map<String, Variant<?>> options = new LinkedHashMap<>();
        options.put("handle_token", new Variant<>(token));
        options.put("modal", new Variant<>(true));
        options.put("accept_label", new Variant<>(pedido.aceitar()));
        options.put("filters", new Variant<>(pedido.filtros().stream().map(PortalDeArquivos::filtro).toList(), "a(sa(us))"));
        options.put("current_filter", new Variant<>(filtro(pedido.filtro()), "(sa(us))"));
        if (pedido.pasta() != null) {
            // Portal paths are filesystem-encoded byte arrays, including the trailing NUL.
            Charset filesystem = Charset.forName(System.getProperty("sun.jnu.encoding", "UTF-8"));
            byte[] path = pedido.pasta().getAbsolutePath().getBytes(filesystem);
            options.put("current_folder", new Variant<>(Arrays.copyOf(path, path.length + 1), "ay"));
        }
        if (pedido.salvar()) options.put("current_name", new Variant<>(pedido.nome()));
        else options.put("multiple", new Variant<>(false));
        return options;
    }

    static String caminhoRequest(String sender, String token) {
        return "/org/freedesktop/portal/desktop/request/" + sender.substring(1).replace('.', '_') + "/" + token;
    }

    static SeletorDeArquivos.Resposta resposta(int code, Map<String, Variant<?>> results,
                                              SeletorDeArquivos.Pedido pedido) {
        if (code == 1) return SeletorDeArquivos.Resposta.cancelar();
        if (code != 0) return SeletorDeArquivos.Resposta.erro();
        try {
            Object uris = results.get("uris").getValue();
            String uri = uris instanceof List<?> list ? (String) list.getFirst() : ((String[]) uris)[0];
            File file = SeletorDeArquivos.deUri(uri);
            SeletorDeArquivos.Filtro selected = pedido.filtro();
            Variant<?> current = results.get("current_filter");
            if (current != null) {
                // Variants deserialize anonymous structs as Object[], rather than our typed Filter.
                Object value = current.getValue();
                Object[] fields = value instanceof Struct struct ? struct.getParameters() : (Object[]) value;
                String name = (String) fields[0];
                selected = pedido.filtros().stream().filter(f -> f.nome().equals(name)).findFirst().orElseThrow();
            }
            return new SeletorDeArquivos.Resposta(SeletorDeArquivos.OK, file, selected);
        } catch (RuntimeException e) {
            return SeletorDeArquivos.Resposta.erro();
        }
    }

    @Override public SeletorDeArquivos.Resposta escolher(Component pai, SeletorDeArquivos.Pedido pedido) throws Exception {
        try (var bus = DBusConnectionBuilder.forSessionBus().withShared(false).build()) {
            // Reading the version also detects absent services/interfaces (and permits D-Bus activation).
            Properties properties = bus.getRemoteObject(DESTINO, CAMINHO, Properties.class);
            UInt32 version = properties.Get(INTERFACE, "version");
            if (version == null || version.longValue() < 1) return SeletorDeArquivos.Resposta.erro();
            FileChooser chooser = bus.getRemoteObject(DESTINO, CAMINHO, FileChooser.class);
            String token = token();
            // AWT exposes neither an X11 XID nor a Wayland exported handle through a public API.
            // An empty parent is permitted by the portal; Swing supplies application modality.
            Request.Response signal = requisitar(bus, token, () -> pedido.salvar()
                    ? chooser.SaveFile("", pedido.titulo(), opcoes(pedido, token))
                    : chooser.OpenFile("", pedido.titulo(), opcoes(pedido, token)));
            return signal == null ? SeletorDeArquivos.Resposta.erro()
                    : resposta(signal.response.intValue(), signal.results, pedido);
        }
    }

    static String token() {
        return "brmodelo_" + UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Calls a portal method that answers through an org.freedesktop.portal.Request and waits
     * for its Response, keeping the connection open meanwhile (closing it cancels the request).
     * {@code chamada} must pass {@code token} as its handle_token. Returns null when the
     * portal returned another request path or the bus or portal went away. Never call on the EDT.
     */
    static Request.Response requisitar(DBusConnection bus, String token, java.util.function.Supplier<DBusPath> chamada)
            throws Exception {
        DBus daemon = bus.getRemoteObject("org.freedesktop.DBus", "/org/freedesktop/DBus", DBus.class);
        String owner = daemon.GetNameOwner(DESTINO);
        String expected = caminhoRequest(bus.getUniqueName(), token);
        CompletableFuture<Request.Response> response = new CompletableFuture<>();
        var rule = DBusMatchRuleBuilder.create().withType(Request.Response.class)
                .withSender(owner).withPath(expected).build();
        // Install the match BEFORE the call: cancellation can race the method reply.
        try (var subscription = bus.addSigHandler(rule, (Request.Response signal) -> response.complete(signal))) {
            DBusPath handle = null;
            boolean completed = false;
            try {
                handle = chamada.get();
                if (!expected.equals(handle.getPath())) return null;
                for (;;) {
                    try {
                        Request.Response signal = response.get(1, TimeUnit.SECONDS);
                        completed = true;
                        return signal;
                    } catch (TimeoutException e) {
                        // Do not time out a user who is still choosing; detect bus/service loss instead.
                        if (!bus.isConnected() || !owner.equals(daemon.GetNameOwner(DESTINO))) return null;
                    }
                }
            } finally {
                // Closing the request dismisses an abandoned dialog, including local wait cancellation.
                if (!completed) {
                    try { bus.getRemoteObject(owner, handle == null ? expected : handle.getPath(), Request.class).Close(); }
                    catch (RuntimeException | DBusException e) { /* The portal may already have removed the request. */ }
                }
            }
        }
    }
}
