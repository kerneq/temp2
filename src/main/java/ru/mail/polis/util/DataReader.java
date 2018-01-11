package ru.mail.polis.util;

import com.google.common.io.ByteStreams;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by iters on 1/11/18.
 */
public class DataReader {

    public static byte[] readData(@NotNull InputStream is) throws IOException {
        return ByteStreams.toByteArray(is);
    }

}
