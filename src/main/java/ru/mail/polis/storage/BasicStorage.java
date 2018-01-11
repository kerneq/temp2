package ru.mail.polis.storage;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.NoSuchElementException;

public class BasicStorage implements Storage {

    private final String dir;

    public BasicStorage(String dir) {
        this.dir = dir;
    }

    @NotNull
    @Override
    public byte[] get(@NotNull String id) throws NoSuchElementException, IllegalArgumentException, IOException {
        Path path = Paths.get(dir, id);
        if (!Files.exists(path)) {
            throw new NoSuchElementException("Invalid ID: " + id);
        }
        return Files.readAllBytes(Paths.get(dir, id));
    }

    @Override
    public void upsert(@NotNull String id, @NotNull byte[] value) throws IllegalArgumentException, IOException {
        Files.write(Paths.get(dir, id), value);
    }

    @Override
    public void delete(@NotNull String id) throws IllegalArgumentException, IOException {
        Files.deleteIfExists(Paths.get(dir, id));
    }

}
