package ru.mail.polis;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Utility classes for handling files
 *
 * @author Vadim Tsesko <mail@incubos.org>
 */
final class Files {
    private static final String TEMP_PREFIX = "highload-kv";

    private Files() {
        // Don't instantiate
    }

    static File createTempDirectory() throws IOException {
        final File data = java.nio.file.Files.createTempDirectory(TEMP_PREFIX).toFile();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (data.exists()) {
                    recursiveDelete(data);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));
        return data;
    }

    static void recursiveDelete(@NotNull final File path) throws IOException {
        java.nio.file.Files.walkFileTree(
                path.toPath(),
                new SimpleFileVisitor<Path>() {
                    private void remove(@NotNull final Path file) throws IOException {
                        if (!file.toFile().delete()) {
                            throw new IOException("Can't delete " + file);
                        }
                    }

                    @NotNull
                    @Override
                    public FileVisitResult visitFile(
                            @NotNull final Path file,
                            @NotNull final BasicFileAttributes attrs) throws IOException {
                        remove(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        remove(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
    }
}
