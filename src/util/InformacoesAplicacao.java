package util;

import java.io.IOException;
import java.util.Properties;

/** NG release metadata is independent of the version embedded in legacy diagrams. */
public final class InformacoesAplicacao {
    private InformacoesAplicacao() {}
    public static String versao() {
        Properties properties = new Properties();
        try (var stream = InformacoesAplicacao.class.getResourceAsStream("/brmodelo-version.properties")) {
            if (stream != null) properties.load(stream);
        } catch (IOException e) {
            BrLogger.Logger("ERROR_VERSION_LOAD", e.getMessage());
        }
        return properties.getProperty("version", "1.0.1");
    }
}
