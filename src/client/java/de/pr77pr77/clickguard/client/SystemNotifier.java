package de.pr77pr77.clickguard.client;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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

    private static void notifyWindows(String title, String message) throws AWTException {
        if (!SystemTray.isSupported()) return;

        SystemTray tray = SystemTray.getSystemTray();
        Image image = Toolkit.getDefaultToolkit().createImage(new byte[0]);

        TrayIcon trayIcon = new TrayIcon(image, "ClickGuard");
        trayIcon.setImageAutoSize(true);
        tray.add(trayIcon);

        trayIcon.displayMessage(title, message, TrayIcon.MessageType.INFO);

        new Thread(() -> {
            try {
                Thread.sleep(5000);
                tray.remove(trayIcon);
            } catch (InterruptedException ignored) {}
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

    private static String getMinecraftWindowTitle() {
        long windowHandle = Minecraft.getInstance().getWindow().handle();
        String title = GLFW.glfwGetWindowTitle(windowHandle);
        return title != null ? title : "Minecraft"; // Fallback
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