package dev.aether.update;

/**
 * Auto-update installer has been removed for security.
 *
 * <p>This app never updates itself. Run {@code git fetch upstream} and review
 * the diff, or download a release manually from the GitHub releases page.
 *
 * <p>The previous implementation silently downloaded and replaced the running
 * jar from a remote GitHub release, spawned external processes to delete old
 * binaries, and showed a popup after exit — all without human review of the
 * code being installed. That flow has been replaced with the read-only
 * {@link UpdateChecker#checkAndNotify()} path, which shows a chat message
 * and link but never writes files or spawns processes.
 */
public final class AutoUpdateInstaller {

    private AutoUpdateInstaller() {
    }

    /**
     * No-op. Previously downloaded and installed updates automatically.
     * Now always delegates to the notify-only {@link UpdateChecker}.
     */
    public static void checkAndInstallLatest() {
        UpdateChecker.checkAndNotify();
    }

    public static String getStatus() {
        return "Disabled. This app does not auto-update. Use git fetch upstream to check for updates.";
    }
}
