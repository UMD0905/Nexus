package com.nexus;

/**
 * Plain launcher class — NOT extending {@link javafx.application.Application}.
 *
 * <p>This indirection is required on Java 9+ when JavaFX is on the module
 * path: the JVM enforces that the class with {@code main()} must not extend
 * {@code Application} directly when launched from an unnamed module.
 * Having a separate launcher bypasses this restriction cleanly.
 */
public class Main {

    public static void main(String[] args) {
        NexusApp.launch(NexusApp.class, args);
    }
}
