package ru.mail.polis;

import org.apache.http.HttpResponse;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Contains utility methods for unit tests
 *
 * @author Vadim Tsesko <mail@incubos.org>
 */
abstract class TestBase {
    private static final int VALUE_LENGTH = 1024;

    static int randomPort() {
        return ThreadLocalRandom.current().nextInt(30000, 40000);
    }

    @NotNull
    static String randomKey() {
        return Long.toHexString(ThreadLocalRandom.current().nextLong());
    }

    @NotNull
    static byte[] randomValue() {
        final byte[] result = new byte[VALUE_LENGTH];
        ThreadLocalRandom.current().nextBytes(result);
        return result;
    }

    @NotNull
    static byte[] payloadOf(@NotNull final HttpResponse response) throws IOException {
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        response.getEntity().writeTo(byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    @NotNull
    static String endpoint(final int port) {
        return "http://localhost:" + port;
    }
}
