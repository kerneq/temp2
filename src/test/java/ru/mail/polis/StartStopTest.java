package ru.mail.polis;

import org.apache.http.client.fluent.Request;
import org.apache.http.conn.HttpHostConnectException;
import org.junit.*;
import org.junit.rules.Timeout;
import org.junit.runners.MethodSorters;

import java.io.File;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * Basic init/deinit test for {@link KVService} implementation
 *
 * @author Vadim Tsesko <mail@incubos.org>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class StartStopTest extends TestBase {
    private static final long TIMEOUT_MS = TimeUnit.SECONDS.toMillis(1);
    private static int port;
    private static File data;
    private static Set<String> endpoints;
    private static KVService storage;
    @Rule
    public final Timeout globalTimeout = Timeout.millis(TIMEOUT_MS * 2);

    @BeforeClass
    public static void beforeAll() throws IOException {
        port = randomPort();
        data = Files.createTempDirectory();
        endpoints = Collections.singleton(endpoint(port));
    }

    @AfterClass
    public static void afterAll() throws IOException {
        Files.recursiveDelete(data);
    }

    private static int status() throws IOException {
        return Request.Get("http://localhost:" + port + "/v0/status")
                .connectTimeout((int) TIMEOUT_MS)
                .socketTimeout((int) TIMEOUT_MS)
                .execute()
                .returnResponse()
                .getStatusLine()
                .getStatusCode();
    }

    @Test
    public void create() throws Exception {
        storage = KVServiceFactory.create(port, data, endpoints);
        try {
            // Should not respond before start
            status();
        } catch (HttpHostConnectException e) {
            // Do nothing
        }
    }

    @Test
    public void start() throws Exception {
        storage.start();
        assertEquals(200, status());
    }

    @Test
    public void stop() throws Exception {
        storage.stop();
        try {
            // Should not respond after stop
            status();
        } catch (SocketTimeoutException | HttpHostConnectException e) {
            // Do nothing
        }
    }
}
