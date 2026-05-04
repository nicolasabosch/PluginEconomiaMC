package com.example.economy.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import org.bukkit.plugin.java.JavaPlugin;

public class AuditLogger {
    private final File logFile;

    public AuditLogger(JavaPlugin plugin) {
        this.logFile = new File(plugin.getDataFolder(), "audit.log");
    }

    public synchronized void log(String line) {
        try {
            if (!logFile.getParentFile().exists()) {
                logFile.getParentFile().mkdirs();
            }
            Files.writeString(
                    logFile.toPath(),
                    Instant.now() + " " + line + System.lineSeparator(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
        }
    }
}
