package de.pr77pr77.clickguard.client;

import net.minecraft.SharedConstants;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import static de.pr77pr77.clickguard.ClickGuard.LOGGER;

public class SystemNotifier {

    public static void notify(String title, String message) {
        String os = System.getProperty("os.name").toLowerCase();

        try {
            if (os.contains("win")) {
                notifyWindows(title, message);
            } else if (os.contains("mac")) {
                notifyMac(title, message);
            } else if (os.contains("nux") || os.contains("nix")) {
                notifyLinux(title, message);
            }
        } catch (Exception e) {
            LOGGER.error("Could not send system notification: ", e);
        }
    }

    public static void notifyWindows(String title, String message) {
        new Thread(() -> {
            try {
                String appName = getMinecraftWindowTitle();

                String safeAppName = appName.replace("'", "''");
                String safeTitle = title.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
                String safeMessage = message.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");

                String script = String.format(
                        "$regPath = 'HKCU:\\Software\\Classes\\AppUserModelId\\MinecraftApp'; " +
                                "if (!(Test-Path $regPath)) { New-Item -Path $regPath -Force | Out-Null; } " +
                                "Set-ItemProperty -Path $regPath -Name 'DisplayName' -Value '%s' -Force | Out-Null; " +
                                "[Windows.UI.Notifications.ToastNotificationManager, Windows.UI.Notifications, ContentType = WindowsRuntime] | Out-Null; " +
                                "[Windows.Data.Xml.Dom.XmlDocument, Windows.Data.Xml.Dom.XmlDocument, ContentType = WindowsRuntime] | Out-Null; " +
                                "$xml = New-Object Windows.Data.Xml.Dom.XmlDocument; " +
                                "$xml.LoadXml('<toast><visual><binding template=\"ToastGeneric\"><text>%s</text><text>%s</text></binding></visual></toast>'); " +
                                "$toast = [Windows.UI.Notifications.ToastNotification]::new($xml); " +
                                "[Windows.UI.Notifications.ToastNotificationManager]::CreateToastNotifier('MinecraftApp').Show($toast); ",
                        safeAppName, safeTitle, safeMessage
                );

                byte[] utf16Bytes = script.getBytes(StandardCharsets.UTF_16LE);
                String encodedScript = Base64.getEncoder().encodeToString(utf16Bytes);

                ProcessBuilder pb = new ProcessBuilder("powershell", "-NoProfile", "-WindowStyle", "Hidden", "-EncodedCommand", encodedScript);
                pb.start();

            } catch (Exception e) {
                LOGGER.error("Could not send system notification: ", e);
            }
        }).start();
    }

    private static void notifyLinux(String title, String message) throws IOException {
        String appName = getMinecraftWindowTitle();

        ProcessBuilder pb = new ProcessBuilder(
                "notify-send",
                "-a", appName,
                title,
                message
        );

        Map<String, String> env = pb.environment();
        String busAddr = env.get("DBUS_SESSION_BUS_ADDRESS");
        if (busAddr == null || busAddr.isEmpty()) {
            env.put("DBUS_SESSION_BUS_ADDRESS", "unix:path=/run/user/" + getLinuxUid() + "/bus");
        }

        pb.start();
    }

    public static String getMinecraftWindowTitle() {
        // Simplified method of getting a title for the notification:
        return "Minecraft " + SharedConstants.getCurrentVersion().name();
    }

    private static volatile String cachedUid = null;

    private static String getLinuxUid() throws IOException {
        if (cachedUid != null) return cachedUid;

        Process process = new ProcessBuilder("id", "-u").start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String uid = reader.readLine();
            if (uid == null || uid.isBlank()) {
                throw new IOException("Could not determine uid");
            }
            cachedUid = uid.trim();
            return cachedUid;
        }
    }

    private static void notifyMac(String title, String message) throws IOException {
        String script = String.format(
                "display notification \"%s\" with title \"%s\"",
                message.replace("\"", "\\\""),
                title.replace("\"", "\\\"")
        );
        new ProcessBuilder("osascript", "-e", script).start();
    }
}