package ru.mail.polis;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.jetbrains.annotations.NotNull;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * Unit tests for single node {@link KVService} API
 *
 * @author Vadim Tsesko <mail@incubos.org>
 */
public class SingleNodeTest extends TestBase {
    private static File data;
    private static String endpoint;
    private static KVService storage;
    @Rule
    public final Timeout globalTimeout = Timeout.seconds(3);

    @BeforeClass
    public static void beforeAll() throws IOException, InterruptedException {
        final int port = randomPort();
        data = Files.createTempDirectory();
        endpoint = endpoint(port);
        storage = KVServiceFactory.create(port, data, Collections.singleton(endpoint));
        storage.start();
    }

    @AfterClass
    public static void afterAll() throws IOException {
        storage.stop();
        Files.recursiveDelete(data);
    }

    @NotNull
    private String url(@NotNull final String id) {
        return endpoint + "/v0/entity?id=" + id;
    }

    private HttpResponse get(@NotNull final String key) throws IOException {
        return Request.Get(url(key)).addHeader("Connection", "Close").execute().returnResponse();
    }

    private HttpResponse delete(@NotNull final String key) throws IOException {
        return Request.Delete(url(key)).addHeader("Connection", "Close").execute().returnResponse();
    }

    private HttpResponse upsert(
            @NotNull final String key,
            @NotNull final byte[] data) throws IOException {
        return Request.Put(url(key)).bodyByteArray(data).addHeader("Connection", "Close").execute().returnResponse();
    }

    @Test
    public void emptyKey() throws Exception {
        assertEquals(400, get("").getStatusLine().getStatusCode());
        assertEquals(400, delete("").getStatusLine().getStatusCode());
        assertEquals(400, upsert("", new byte[]{0}).getStatusLine().getStatusCode());
    }

    @Test
    public void badRequest() throws Exception {
        assertEquals(
                404,
                Request.Get(url("/abracadabra")).execute().returnResponse()
                        .getStatusLine().getStatusCode());
    }

    @Test
    public void getAbsent() throws Exception {
        assertEquals(
                404,
                get("absent").getStatusLine().getStatusCode());
    }

    @Test
    public void deleteAbsent() throws Exception {
        assertEquals(202, delete("absent").getStatusLine().getStatusCode());
    }

    @Test
    public void insert() throws Exception {
        final String key = randomKey();
        final byte[] value = randomValue();
        System.out.println(value.length + " is it" );
        // Insert
        assertEquals(201, upsert(key, value).getStatusLine().getStatusCode());

        // Check
        final HttpResponse response = get(key);
        assertEquals(200, response.getStatusLine().getStatusCode());
        assertArrayEquals(value, payloadOf(response));
    }

    @Test
    public void insertEmpty() throws Exception {
        final String key = randomKey();
        final byte[] value = new byte[0];

        // Insert
        assertEquals(201, upsert(key, value).getStatusLine().getStatusCode());

        // Check
        final HttpResponse response = get(key);
        assertEquals(200, response.getStatusLine().getStatusCode());
        assertArrayEquals(value, payloadOf(response));
    }

    @Test
    public void lifecycle2keys() throws Exception {
        final String key1 = randomKey();
        final byte[] value1 = randomValue();
        final String key2 = randomKey();
        final byte[] value2 = randomValue();

        // Insert 1
        assertEquals(201, upsert(key1, value1).getStatusLine().getStatusCode());

        // Check
        assertArrayEquals(value1, payloadOf(get(key1)));

        // Insert 2
        assertEquals(201, upsert(key2, value2).getStatusLine().getStatusCode());

        // Check
        assertArrayEquals(value1, payloadOf(get(key1)));
        assertArrayEquals(value2, payloadOf(get(key2)));

        // Delete 1
        assertEquals(202, delete(key1).getStatusLine().getStatusCode());

        // Check
        assertEquals(404, get(key1).getStatusLine().getStatusCode());
        assertArrayEquals(value2, payloadOf(get(key2)));

        // Delete 2
        assertEquals(202, delete(key2).getStatusLine().getStatusCode());

        // Check
        assertEquals(404, get(key2).getStatusLine().getStatusCode());
    }

    @Test
    public void upsert() throws Exception {
        final String key = randomKey();
        final byte[] value1 = randomValue();
        final byte[] value2 = randomValue();

        // Insert value1
        assertEquals(201, upsert(key, value1).getStatusLine().getStatusCode());

        // Insert value2
        assertEquals(201, upsert(key, value2).getStatusLine().getStatusCode());

        // Check value 2
        final HttpResponse response = get(key);
        assertEquals(200, response.getStatusLine().getStatusCode());
        assertArrayEquals(value2, payloadOf(response));
    }

    @Test
    public void respectFileFolder() throws Exception {
        final String key = randomKey();
        final byte[] value = randomValue();

        // Insert value
        assertEquals(201, upsert(key, value).getStatusLine().getStatusCode());

        // Check value
        final HttpResponse response = get(key);
        assertEquals(200, response.getStatusLine().getStatusCode());
        assertArrayEquals(value, payloadOf(response));

        // Remove data and recreate
        storage.stop();
        Files.recursiveDelete(data);
        final int port = randomPort();
        java.nio.file.Files.createDirectory(data.toPath());
        endpoint = endpoint(port);
        storage = KVServiceFactory.create(port, data, Collections.singleton(endpoint));
        storage.start();

        // Check absent data
        assertEquals(404, get(key).getStatusLine().getStatusCode());
    }

    @Test
    public void upsertEmpty() throws Exception {
        final String key = randomKey();
        final byte[] value = randomValue();
        final byte[] empty = new byte[0];

        // Insert value
        assertEquals(201, upsert(key, value).getStatusLine().getStatusCode());

        // Insert empty
        assertEquals(201, upsert(key, empty).getStatusLine().getStatusCode());

        // Check empty
        final HttpResponse response = get(key);
        assertEquals(200, response.getStatusLine().getStatusCode());
        assertArrayEquals(empty, payloadOf(response));
    }

    @Test
    public void delete() throws Exception {
        final String key = randomKey();
        final byte[] value = randomValue();

        // Insert
        assertEquals(201, upsert(key, value).getStatusLine().getStatusCode());

        // Delete
        assertEquals(202, delete(key).getStatusLine().getStatusCode());

        // Check
        assertEquals(404, get(key).getStatusLine().getStatusCode());
    }
}
