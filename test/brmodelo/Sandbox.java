package brmodelo;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Tests of the real bubblewrap sandbox skip where it cannot run, except where it is required. */
public final class Sandbox {
    private Sandbox() {}

    /** With -PexigirIsolamento (CI) an unavailable sandbox fails the test instead of skipping it. */
    public static void disponivel(boolean funciona, String motivo) {
        if (Boolean.getBoolean("brmodelo.exigirIsolamento")) assertTrue(funciona, motivo + " (required here)");
        else assumeTrue(funciona, motivo);
    }
}
