package ru.mail.polis;

import org.apache.http.HttpResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * Unit tests for a two node {@link KVService} cluster
 *
 * @author Vadim Tsesko <mail@incubos.org>
 */
public class TwoNodeTest extends ClusterTestBase {
    @Rule
    public final Timeout globalTimeout = Timeout.seconds(3);
    private int port0;
    private int port1;
    private File data0;
    private File data1;
    private KVService storage0;
    private KVService storage1;

    @Before
    public void beforeEach() throws IOException, InterruptedException {
        port0 = randomPort();
        port1 = randomPort();
        endpoints = new LinkedHashSet<>(Arrays.asList(endpoint(port0), endpoint(port1)));
        data0 = Files.createTempDirectory();
        data1 = Files.createTempDirectory();
        storage0 = KVServiceFactory.create(port0, data0, endpoints);
        storage0.start();
        storage1 = KVServiceFactory.create(port1, data1, endpoints);
        storage1.start();
    }

    @After
    public void afterEach() throws IOException {
        storage0.stop();
        Files.recursiveDelete(data0);
        storage1.stop();
        Files.recursiveDelete(data1);
        endpoints = Collections.emptySet();
    }

    @Test
    public void tooSmallRF() throws Exception {
        assertEquals(400, get(0, randomKey(), 0, 2).getStatusLine().getStatusCode());
        assertEquals(400, upsert(0, randomKey(), randomValue(), 0, 2).getStatusLine().getStatusCode());
        assertEquals(400, delete(0, randomKey(), 0, 2).getStatusLine().getStatusCode());
    }

    @Test
    public void tooBigRF() throws Exception {
        assertEquals(400, get(0, randomKey(), 3, 2).getStatusLine().getStatusCode());
        assertEquals(400, upsert(0, randomKey(), randomValue(), 3, 2).getStatusLine().getStatusCode());
        assertEquals(400, delete(0, randomKey(), 3, 2).getStatusLine().getStatusCode());
    }

    @Test
    public void unreachableRF() throws Exception {
        storage0.stop();
        assertEquals(504, get(1, randomKey(), 2, 2).getStatusLine().getStatusCode());
        assertEquals(504, upsert(1, randomKey(), randomValue(), 2, 2).getStatusLine().getStatusCode());
        assertEquals(504, delete(1, randomKey(), 2, 2).getStatusLine().getStatusCode());
    }

    @Test
    public void overlapRead() throws Exception {
        final String key = randomKey();
        final byte[] value = randomValue();

        // Insert
        assertEquals(201, upsert(0, key, value, 1, 2).getStatusLine().getStatusCode());

        // Check
        final HttpResponse response = get(1, key, 2, 2);
        assertEquals(200, response.getStatusLine().getStatusCode());
        assertArrayEquals(value, payloadOf(response));
    }

    @Test
    public void overlapWrite() throws Exception {
        final String key = randomKey();
        final byte[] value = randomValue();

        // Insert
        assertEquals(201, upsert(0, key, value, 2, 2).getStatusLine().getStatusCode());

        // Check
        final HttpResponse response = get(1, key, 1, 2);
        assertEquals(200, response.getStatusLine().getStatusCode());
        assertArrayEquals(value, payloadOf(response));
    }

    @Test
    public void overlapDelete() throws Exception {
        final String key = randomKey();
        final byte[] value = randomValue();

        // Insert
        assertEquals(201, upsert(0, key, value, 2, 2).getStatusLine().getStatusCode());

        // Check
        HttpResponse response = get(1, key, 1, 2);
        assertEquals(200, response.getStatusLine().getStatusCode());
        assertArrayEquals(value, payloadOf(response));

        // Delete
        assertEquals(202, delete(0, key, 2, 2).getStatusLine().getStatusCode());

        // Check
        response = get(1, key, 1, 2);
        assertEquals(404, response.getStatusLine().getStatusCode());
    }

    @Test
    public void missedWrite() throws Exception {
        final String key = randomKey();
        final byte[] value = randomValue();

        // Stop node 1
        storage1.stop();

        // Insert
        assertEquals(201, upsert(0, key, value, 1, 2).getStatusLine().getStatusCode());

        // Start node 1
        storage1 = KVServiceFactory.create(port1, data1, endpoints);
        storage1.start();

        // Check
        final HttpResponse response = get(1, key, 2, 2);
        assertEquals(200, response.getStatusLine().getStatusCode());
        assertArrayEquals(value, payloadOf(response));
    }

    @Test
    public void missedDelete() throws Exception {
        final String key = randomKey();
        final byte[] value = randomValue();

        // Insert
        assertEquals(201, upsert(0, key, value, 2, 2).getStatusLine().getStatusCode());

        // Stop node 0
        storage0.stop();

        // Delete
        assertEquals(202, delete(1, key, 1, 2).getStatusLine().getStatusCode());

        // Start node 0
        storage0 = KVServiceFactory.create(port0, data0, endpoints);
        storage0.start();

        // Check
        final HttpResponse response = get(0, key, 2, 2);
        assertEquals(404, response.getStatusLine().getStatusCode());
    }

    @Test
    public void respectRF() throws Exception {
        final String key = randomKey();
        final byte[] value = randomValue();

        // Insert
        assertEquals(201, upsert(0, key, value, 1, 1).getStatusLine().getStatusCode());

        int copies = 0;

        // Stop node 0
        storage0.stop();

        // Check
        if (get(1, key, 1, 1).getStatusLine().getStatusCode() == 200) {
            copies++;
        }

        // Start node 0
        storage0 = KVServiceFactory.create(port0, data0, endpoints);
        storage0.start();

        // Stop node 1
        storage1.stop();

        // Check
        if (get(0, key, 1, 1).getStatusLine().getStatusCode() == 200) {
            copies++;
        }

        // Start node 1
        storage1 = KVServiceFactory.create(port1, data1, endpoints);
        storage1.start();

        // Check
        assertEquals(1, copies);
    }
}
