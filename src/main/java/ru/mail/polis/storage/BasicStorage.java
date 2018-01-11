package ru.mail.polis.storage;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.NoSuchElementException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class BasicStorage implements Storage {

    private final String dir;
    private final Executor exec;

    public BasicStorage(String dir) {
        this.dir = dir;
        exec = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
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
        // то может ускорить вставку данных, но мы можем получить
        // java.nio.file.NoSuchFileException когда быстро обращаемся к
        // вставляемому файлу

        //exec.execute(() -> {
            try {
                Files.write(Paths.get(dir, id), value);
            } catch (IOException e) {
                e.printStackTrace();
            }
        //});
    }

    @Override
    public void delete(@NotNull String id) throws IllegalArgumentException, IOException {
        exec.execute(() -> {
            try {
                Files.deleteIfExists(Paths.get(dir, id));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

}
