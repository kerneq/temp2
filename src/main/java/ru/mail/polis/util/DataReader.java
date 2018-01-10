package ru.mail.polis.util;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by iters on 1/11/18.
 */
public class DataReader {

    private static final int BUFFER_SIZE = 1024;

    public static byte[] readData(@NotNull InputStream is) throws IOException {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[BUFFER_SIZE];
            for (int len; (len = is.read(buffer, 0, BUFFER_SIZE)) != -1; ) {
                os.write(buffer, 0, len);
            }
            os.flush();
            return os.toByteArray();
        }
    }
}
