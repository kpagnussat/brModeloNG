package util;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Stands in for the import worker in TrabalhadorImportacaoTest: it announces itself like the
 * worker and then misbehaves on purpose, or reports what its sandbox lets it reach.
 */
public final class SondaTrabalhador {
    public static void main(String[] args) throws Exception {
        System.out.write(TrabalhadorImportacao.PRONTO);
        System.out.flush();
        switch (args[0]) {
            case "lento" -> { while (true) Thread.onSpinWait(); }
            case "memoria" -> {
                List<long[]> blocos = new ArrayList<>();
                while (true) blocos.add(new long[1 << 20]);
            }
            case "eco" -> System.in.transferTo(System.out);
            case "isolamento" -> {
                System.out.println("arquivo: " + tentar(() -> Files.readAllBytes(Path.of(args[1])).length + " bytes"));
                System.out.println("rede: " + tentar(() -> {
                    try (Socket s = new Socket()) {
                        s.connect(new InetSocketAddress("127.0.0.1", Integer.parseInt(args[2])), 2000);
                        return "conectou";
                    }
                }));
                System.out.println("tela: " + System.getenv("DISPLAY") + " " + Files.exists(Path.of("/tmp/.X11-unix")));
                System.out.println("dbus: " + System.getenv("DBUS_SESSION_BUS_ADDRESS") + " " + Files.exists(Path.of(args[3])));
            }
            default -> throw new IllegalArgumentException(args[0]);
        }
        System.out.flush();
    }

    interface Acao { String rodar() throws Exception; }

    private static String tentar(Acao acao) {
        try { return acao.rodar(); } catch (Exception e) { return "bloqueado (" + e.getClass().getSimpleName() + ")"; }
    }
}
