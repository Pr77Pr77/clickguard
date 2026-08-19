package de.pr77pr77.clickguard.client;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.win32.StdCallLibrary;
import net.minecraft.SharedConstants;

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


    // Windows:
    private interface User32 extends StdCallLibrary {
        User32 INSTANCE = Native.load("user32", User32.class);

        Pointer GetForegroundWindow();

        Pointer LoadIconW(Pointer hInstance, Pointer lpIconName);
    }

    private interface Shell32 extends StdCallLibrary {
        Shell32 INSTANCE = Native.load("shell32", Shell32.class);

        boolean Shell_NotifyIconW(int dwMessage, NOTIFYICONDATAW lpData);
    }

    @Structure.FieldOrder({
            "cbSize", "hWnd", "uID", "uFlags", "uCallbackMessage",
            "hIcon", "szTip", "dwState", "dwStateMask",
            "szInfo", "uTimeoutOrVersion", "szInfoTitle", "dwInfoFlags"
    })
    public static class NOTIFYICONDATAW extends Structure {
        public int cbSize;
        public Pointer hWnd;
        public int uID = 1001;
        public int uFlags = 0;
        public int uCallbackMessage = 0x0401; // WM_USER + 1
        public Pointer hIcon = null;
        public char[] szTip = new char[128];
        public int dwState = 0;
        public int dwStateMask = 0;
        public char[] szInfo = new char[256];
        public int uTimeoutOrVersion = 10000;
        public char[] szInfoTitle = new char[64];
        public int dwInfoFlags = 1; // NIIF_INFO

        public NOTIFYICONDATAW() {
            this.cbSize = this.size();
        }
    }

    public static void notifyWindows(String title, String message) {
        new Thread(() -> {
            try {
                NOTIFYICONDATAW data = new NOTIFYICONDATAW();

                // Set window
                data.hWnd = User32.INSTANCE.GetForegroundWindow();

                // Only register icon
                data.uFlags = 0x02 | 0x04; // NIF_ICON | NIF_TIP
                data.hIcon = User32.INSTANCE.LoadIconW(null, new Pointer(32516)); // Info-Symbol

                char[] tipChars = "ClickGuard".toCharArray();
                System.arraycopy(tipChars, 0, data.szTip, 0, tipChars.length);

                // NIM_ADD (0): create icon
                Shell32.INSTANCE.Shell_NotifyIconW(0, data);

                Thread.sleep(150);

                // Add notification
                data.uFlags = 0x10;

                char[] titleChars = title.toCharArray();
                System.arraycopy(titleChars, 0, data.szInfoTitle, 0, Math.min(titleChars.length, 63));

                char[] msgChars = message.toCharArray();
                System.arraycopy(msgChars, 0, data.szInfo, 0, Math.min(msgChars.length, 255));

                // NIM_MODIFY (1) - Fire toast
                Shell32.INSTANCE.Shell_NotifyIconW(1, data);

                Thread.sleep(30000);

                // NIM_DELETE (2)
                Shell32.INSTANCE.Shell_NotifyIconW(2, data);
            } catch (Throwable t) {
                LOGGER.error("Could not send system notification: ", t);
            }
        }).start();
    }

    // Linux:
    private interface LibNotify extends Library {
        LibNotify INSTANCE = Native.load("notify", LibNotify.class);

        boolean notify_init(String appName);

        Pointer notify_notification_new(String summary, String body, String icon);

        void notify_notification_show(Pointer notification, Pointer error);
    }

    public static void notifyLinux(String title, String message) {
        new Thread(() -> {
            try {
                if (LibNotify.INSTANCE.notify_init(getMinecraftWindowTitle())) {
                    Pointer notification = LibNotify.INSTANCE.notify_notification_new(
                            title,
                            message,
                            "applications-games"
                    );

                    LibNotify.INSTANCE.notify_notification_show(notification, null);
                }
            } catch (Throwable t) {
                LOGGER.error("Could not send system notification: ", t);
            }
        }).start();
    }

    // Mac OS:

    private interface ObjC extends Library {
        ObjC INSTANCE = Native.load("objc", ObjC.class);

        Pointer objc_getClass(String name);

        Pointer sel_registerName(String name);

        Pointer objc_msgSend(Pointer receiver, Pointer selector);

        Pointer objc_msgSend(Pointer receiver, Pointer selector, Pointer arg1);

        Pointer objc_msgSend(Pointer receiver, Pointer selector, String arg1);
    }

    public static void notifyMac(String title, String message) {
        new Thread(() -> {
            try {
                ObjC runtime = ObjC.INSTANCE;

                // NSUserNotification alloc & init
                Pointer notificationClass = runtime.objc_getClass("NSUserNotification");
                Pointer allocSel = runtime.sel_registerName("alloc");
                Pointer initSel = runtime.sel_registerName("init");

                Pointer allocated = runtime.objc_msgSend(notificationClass, allocSel);
                Pointer notification = runtime.objc_msgSend(allocated, initSel);

                // generate NSStrings
                Pointer stringClass = runtime.objc_getClass("NSString");
                Pointer stringWithUtf8Sel = runtime.sel_registerName("stringWithUTF8String:");

                Pointer nsTitle = runtime.objc_msgSend(stringClass, stringWithUtf8Sel, title);
                Pointer nsMessage = runtime.objc_msgSend(stringClass, stringWithUtf8Sel, message);

                // set values
                Pointer setTitleSel = runtime.sel_registerName("setTitle:");
                Pointer setInformativeTextSel = runtime.sel_registerName("setInformativeText:");

                runtime.objc_msgSend(notification, setTitleSel, nsTitle);
                runtime.objc_msgSend(notification, setInformativeTextSel, nsMessage);

                // send notification
                Pointer centerClass = runtime.objc_getClass("NSUserNotificationCenter");
                Pointer defaultCenterSel = runtime.sel_registerName("defaultUserNotificationCenter");
                Pointer center = runtime.objc_msgSend(centerClass, defaultCenterSel);

                Pointer deliverNotificationSel = runtime.sel_registerName("deliverNotification:");
                runtime.objc_msgSend(center, deliverNotificationSel, notification);
            } catch (Throwable t) {
                LOGGER.error("Could not send system notification: ", t);
            }
        }).start();
    }

    public static String getMinecraftWindowTitle() {
        // Simplified method of getting a title for the notification:
        return "Minecraft " + SharedConstants.getCurrentVersion().name();
    }
}