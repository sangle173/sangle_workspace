package com.example.local_cloud.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class CleanupService {
    private static final Logger logger = LoggerFactory.getLogger(CleanupService.class);

    @Value("${notes.base-path}")
    private String basePath;

    @Scheduled(cron = "0 0 * * * *") // Run every hour
    public void cleanupOldFiles() {
        logger.info("Starting scheduled cleanup of old files");
        try {
            Path baseDir = Paths.get(basePath);
            if (!Files.exists(baseDir)) {
                return;
            }

            Files.walkFileTree(baseDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    try {
                        // Skip note.json files
                        if (file.getFileName().toString().equals("note.json")) {
                            return FileVisitResult.CONTINUE;
                        }

                        // Check if file is an attachment that hasn't been accessed in 24 hours
                        if (attrs.lastAccessTime().toInstant().plus(24, ChronoUnit.HOURS)
                                .isBefore(Instant.now())) {
                            // If file is in an attachments directory and not referenced in the note
                            if (isUnusedAttachment(file)) {
                                Files.deleteIfExists(file);
                                logger.info("Deleted unused attachment: {}", file);
                            }
                        }
                    } catch (IOException e) {
                        logger.error("Error processing file during cleanup: {}", file, e);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    logger.error("Failed to access file: {}", file, exc);
                    return FileVisitResult.CONTINUE;
                }
            });

            logger.info("Completed scheduled cleanup");
        } catch (IOException e) {
            logger.error("Error during scheduled cleanup", e);
        }
    }

    private boolean isUnusedAttachment(Path file) {
        try {
            // Check if file is in attachments directory
            if (!file.toString().contains("attachments")) {
                return false;
            }

            // Get the note.json file for this attachment
            Path noteDir = file.getParent().getParent();
            Path noteJson = noteDir.resolve("note.json");

            // If note.json doesn't exist, consider the attachment unused
            if (!Files.exists(noteJson)) {
                return true;
            }

            // Read note content and check if file is referenced
            String noteContent = Files.readString(noteJson);
            String fileName = file.getFileName().toString();
            return !noteContent.contains(fileName);

        } catch (IOException e) {
            logger.error("Error checking if attachment is unused: {}", file, e);
            return false;
        }
    }
}