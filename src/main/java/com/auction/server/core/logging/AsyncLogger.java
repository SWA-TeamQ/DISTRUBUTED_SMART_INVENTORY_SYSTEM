package com.auction.server.core.logging;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class AsyncLogger {
    private static final String LOGS_DIR = "logs";
    
    // Use a singleton pattern since business code shouldn't care about instantiation.
    private static AsyncLogger instance;
    
    private final BlockingQueue<LogEntry> queue = new LinkedBlockingQueue<>();
    private final Map<LogCategory, BufferedWriter> writers = new EnumMap<>(LogCategory.class);
    private final Thread workerThread;
    private volatile boolean isRunning = true;

    private AsyncLogger() {
        try {
            Files.createDirectories(Paths.get(LOGS_DIR));
            for (LogCategory category : LogCategory.values()) {
                Path logPath = Paths.get(LOGS_DIR, category.name().toLowerCase() + ".log");
                writers.put(category, new BufferedWriter(new FileWriter(logPath.toFile(), true)));
            }
        } catch (IOException e) {
            System.err.println("Failed to initialize AsyncLogger writers: " + e.getMessage());
        }

        workerThread = new Thread(this::processQueue, "LoggerWorker");
        workerThread.setDaemon(true); // Don't block JVM shutdown
        workerThread.start();

        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
        
        log(LogCategory.SYSTEM, EventType.SERVER_START, "AsyncLogger initialized");
    }

    public static synchronized void initialize() {
        if (instance == null) {
            instance = new AsyncLogger();
        }
    }

    public static void log(LogCategory category, EventType eventType, String details) {
        if (instance != null) {
            instance.queue.offer(new LogEntry(category, eventType, details));
        } else {
            // Fallback if not initialized
            System.err.println("[" + category + "] " + eventType + ": " + details);
        }
    }

    private void processQueue() {
        while (isRunning || !queue.isEmpty()) {
            try {
                LogEntry entry = queue.take(); // Blocks until an entry is available
                writeEntry(entry);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                if (!isRunning) break;
            }
        }
    }

    private void writeEntry(LogEntry entry) {
        BufferedWriter writer = writers.get(entry.getCategory());
        if (writer != null) {
            try {
                String formattedLog = String.format("%s | %s | %s",
                        entry.getTimestamp().toString(),
                        entry.getEventType().name(),
                        entry.getDetails());
                writer.write(formattedLog);
                writer.newLine();
                writer.flush();
            } catch (IOException e) {
                System.err.println("Failed to write log entry: " + e.getMessage());
            }
        }
    }

    private void shutdown() {
        isRunning = false;
        workerThread.interrupt();
        try {
            workerThread.join(5000); // Wait for worker to finish writing queue
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Flush and close all writers
        for (BufferedWriter writer : writers.values()) {
            try {
                if (writer != null) {
                    writer.flush();
                    writer.close();
                }
            } catch (IOException e) {
                System.err.println("Error closing log writer: " + e.getMessage());
            }
        }
    }
}
