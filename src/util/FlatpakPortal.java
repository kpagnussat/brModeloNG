package util;

import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.types.UInt32;

/** What the org.freedesktop.portal.Flatpak service of this Flatpak session supports. */
final class FlatpakPortal {
    private FlatpakPortal() {}

    // FLATPAK_SPAWN_SUPPORT_FLAGS_EXPOSE_PIDS: absent with a setuid bwrap, where the portal
    // refuses the --expose-pids spawn flag outright.
    private static final long EXPOE_PIDS = 1;
    private static Boolean expoePids;

    /** Whether flatpak-spawn --expose-pids will be accepted; false when the portal cannot say. */
    static synchronized boolean expoePids() {
        if (expoePids == null) {
            try (var bus = DBusConnectionBuilder.forSessionBus().withShared(false).build()) {
                UInt32 supports = bus.getRemoteObject("org.freedesktop.portal.Flatpak", "/org/freedesktop/portal/Flatpak",
                        Properties.class).Get("org.freedesktop.portal.Flatpak", "supports");
                expoePids = supports != null && (supports.longValue() & EXPOE_PIDS) != 0;
            } catch (Exception | LinkageError e) {
                expoePids = false;
            }
        }
        return expoePids;
    }
}
