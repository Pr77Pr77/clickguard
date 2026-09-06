package de.pr77pr77.clickguard.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class SystemNotifierTest {

    @Test
    public void testGeneralNotifyCall() {
        // Checks, if notifications run without exceptions.
        assertDoesNotThrow(() -> {
            SystemNotifier.notify("Notification Test", "This is a test notification!");
            // Sleep is needed, as the notification is sent from another thread.
            Thread.sleep(1000);
        });
    }
}