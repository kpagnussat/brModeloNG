package util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/** Update only the appearance key, preserving every other byte of an existing config file. */
public final class ConfiguracaoTema {
    private ConfiguracaoTema() {}
    public static void salvar(Path path, String value) throws IOException {
        salvar(path, TemaAplicacao.CHAVE, value);
    }
    public static void salvar(Path path, String key, String value) throws IOException {
        String source = Files.exists(path) ? Files.readString(path, StandardCharsets.ISO_8859_1) : "";
        String replacement = key + "=" + value;
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(?m)^" + java.util.regex.Pattern.quote(key) + "[ \\t]*[=:][^\\r\\n]*");
        java.util.regex.Matcher matcher = pattern.matcher(source);
        String result;
        if (matcher.find()) result = matcher.replaceFirst(java.util.regex.Matcher.quoteReplacement(replacement));
        else {
            String newline = source.contains("\r\n") ? "\r\n" : "\n";
            result = source + (source.isEmpty() || source.endsWith("\n") || source.endsWith("\r") ? "" : newline)
                    + replacement + newline;
        }
        GravacaoAtomica.gravar(path, saida -> saida.write(result.getBytes(StandardCharsets.ISO_8859_1)));
    }
}
