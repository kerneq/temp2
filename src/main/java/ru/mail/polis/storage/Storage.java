package ru.mail.polis.storage;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.NoSuchElementException;

public interface Storage {

    @NotNull
    byte[] get(@NotNull String id) throws NoSuchElementException, IllegalArgumentException, IOException;

    void upsert(@NotNull String id, @NotNull byte[] value) throws IllegalArgumentException, IOException;

    void delete(@NotNull String id) throws IllegalArgumentException, IOException;

}
