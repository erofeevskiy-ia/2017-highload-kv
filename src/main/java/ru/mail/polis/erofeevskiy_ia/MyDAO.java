package ru.mail.polis.erofeevskiy_ia;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.NoSuchElementException;

public interface MyDAO {
    @NotNull
    byte[] get(String key) throws NoSuchElementException, IllegalArgumentException, IOException;

    void upsert(@NotNull String key, @NotNull byte[] value) throws IllegalArgumentException, IOException;

    @NotNull
    void delete(String key) throws IllegalArgumentException, IOException;
}
