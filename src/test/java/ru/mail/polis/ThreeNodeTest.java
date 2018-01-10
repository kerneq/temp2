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
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * Unit tests for a three node {@link KVService} cluster
 *
 * @author Vadim Tsesko <mail@incubos.org>
 */
public class ThreeNodeTest extends ClusterTestBase {
    @Rule
    public final Timeout globalTimeout = Timeout.seconds(10);
    private int port0;
    private int port1;
    private int port2;
    private File data0;
    private File data1;
    private File data2;
    private KVService storage0;
    private KVService storage1;
    private KVService storage2;

    @Before
    public void beforeEach() throws IOException, InterruptedException {
        port0 = randomPort();
        port1 = randomPort();
        port2 = randomPort();
        endpoints = new LinkedHashSet<>(Arrays.asList(endpoint(port0), endpoint(port1), endpoint(port2)));
        data0 = Files.createTempDirectory();
        data1 = Files.createTempDirectory();
        data2 = Files.createTempDirectory();
        storage0 = KVServiceFactory.create(port0, data0, endpoints);
        storage0.start();
        storage1 = KVServiceFactory.create(port1, data1, endpoints);
        storage1.start();
        storage2 = KVServiceFactory.create(port2, data2, endpoints);
        storage2.start();
    }

    @After
    public void afterEach() throws IOException {
        storage0.stop();
        Files.recursiveDelete(data0);
        storage1.stop();
        Files.recursiveDelete(data1);
        storage2.stop();
        Files.recursiveDelete(data2);
        endpoints = Collections.emptySet();
    }

    @Test
    public void tooSmallRF() throws Exception {
        assertEquals(400, get(0, randomKey(), 0, 3).getStatusLine().getStatusCode());
        assertEquals(400, upsert(0, randomKey(), randomValue(), 0, 3).getStatusLine().getStatusCode());
        assertEquals(400, delete(0, randomKey(), 0, 3).getStatusLine().getStatusCode());
    }

    @Test
    public void tooBigRF() throws Exception {
        assertEquals(400, get(0, randomKey(), 4, 3).getStatusLine().getStatusCode());
        assertEquals(400, upsert(0, randomKey(), randomValue(), 4, 3).getStatusLine().getStatusCode());
        assertEquals(400, delete(0, randomKey(), 4, 3).getStatusLine().getStatusCode());
    }

    @Test
    public void unreachableRF() throws Exception {
        storage0.stop();
        assertEquals(504, get(1, randomKey(), 3, 3).getStatusLine().getStatusCode());
        assertEquals(504, upsert(1, randomKey(), randomValue(), 3, 3).getStatusLine().getStatusCode());
        assertEquals(504, delete(1, randomKey(), 3, 3).getStatusLine().getStatusCode());
    }

    @Test
    public void overlapRead() throws Exception {
        final String key = randomKey();
        final byte[] value = randomValue();

        // Insert
        assertEquals(201, upsert(0, key, value, 2, 3).getStatusLine().getStatusCode());

        // Check
        final HttpResponse response = get(1, key, 2, 3);
        assertEquals(200, response.getStatusLine().getStatusCode());
        assertArrayEquals(value, payloadOf(response));
    }

    @Test
    public void overlapWrite() throws Exception {
        final String key = randomKey();
        final byte[] value = randomValue();

        // Insert
        assertEquals(201, upsert(0, key, value, 2, 3).getStatusLine().getStatusCode());

        // Check
        final HttpResponse response = get(1, key, 2, 3);
        assertEquals(200, response.getStatusLine().getStatusCode());
        assertArrayEquals(value, payloadOf(response));
    }

    @Test
    public void overwrite() throws Exception {
        final String key = randomKey();
        final byte[] value1 = randomValue();
        final byte[] value2 = randomValue();

        // Insert 1
        assertEquals(201, upsert(0, key, value1, 2, 3).getStatusLine().getStatusCode());

        // Check 1
        HttpResponse response = get(1, key, 2, 3);
        assertEquals(200, response.getStatusLine().getStatusCode());
        assertArrayEquals(value1, payloadOf(response));

        // Help implementors with second precision for conflict resolution
        Thread.sleep(TimeUnit.SECONDS.toMillis(1));

        // Insert 2
        assertEquals(201, upsert(2, key, value2, 2, 3).getStatusLine().getStatusCode());

        // Check 2
        response = get(1, key, 2, 3);
        assertEquals(200, response.getStatusLine().getStatusCode());
        assertArrayEquals(value2, payloadOf(response));
    }

    @Test
    public void overlapDelete() throws Exception {
        final String key = randomKey();
        final byte[] value = randomValue();

        // Insert
        assertEquals(201, upsert(0, key, value, 2, 3).getStatusLine().getStatusCode());

        // Check
        HttpResponse response = get(1, key, 2, 3);
        assertEquals(200, response.getStatusLine().getStatusCode());
        assertArrayEquals(value, payloadOf(response));

        // Delete
        assertEquals(202, delete(0, key, 2, 3).getStatusLine().getStatusCode());

        // Check
        response = get(1, key, 2, 3);
        assertEquals(404, response.getStatusLine().getStatusCode());
    }

    @Test
    public void missedWrite() throws Exception {
        final String key = randomKey();
        final byte[] value = randomValue();

        // Stop node 1
        storage1.stop();

        // Insert
        assertEquals(201, upsert(0, key, value, 2, 3).getStatusLine().getStatusCode());

        // Start node 1
        storage1 = KVServiceFactory.create(port1, data1, endpoints);
        storage1.start();

        // Check
        final HttpResponse response = get(1, key, 2, 3);
        assertEquals(200, response.getStatusLine().getStatusCode());
        assertArrayEquals(value, payloadOf(response));
    }

    @Test
    public void missedDelete() throws Exception {
        final String key = randomKey();
        final byte[] value = randomValue();

        // Insert
        assertEquals(201, upsert(0, key, value, 2, 3).getStatusLine().getStatusCode());

        // Stop node 0
        storage0.stop();

        // Delete
        assertEquals(202, delete(1, key, 2, 3).getStatusLine().getStatusCode());

        // Start node 0
        storage0 = KVServiceFactory.create(port0, data0, endpoints);
        storage0.start();

        // Check
        final HttpResponse response = get(0, key, 2, 3);
        assertEquals(404, response.getStatusLine().getStatusCode());
    }

    @Test
    public void tolerateFailure() throws Exception {
        final String key = randomKey();
        final byte[] value = randomValue();

        // Insert into node 2
        assertEquals(201, upsert(2, key, value, 2, 3).getStatusLine().getStatusCode());

        // Stop node 2
        storage2.stop();

        // Check
        HttpResponse response = get(1, key, 2, 3);
        assertEquals(200, response.getStatusLine().getStatusCode());
        assertArrayEquals(value, payloadOf(response));

        // Delete
        assertEquals(202, delete(0, key, 2, 3).getStatusLine().getStatusCode());

        // Check
        response = get(1, key, 2, 3);
        assertEquals(404, response.getStatusLine().getStatusCode());
    }

    @Test
    public void respectRF1() throws Exception {
        final String key = randomKey();
        final byte[] value = randomValue();

        // Insert
        assertEquals(201, upsert(0, key, value, 1, 1).getStatusLine().getStatusCode());

        int copies = 0;

        // Stop all nodes
        storage0.stop();
        storage1.stop();
        storage2.stop();

        // Start node 0
        storage0 = KVServiceFactory.create(port0, data0, endpoints);
        storage0.start();

        // Check node 0
        if (get(0, key, 1, 1).getStatusLine().getStatusCode() == 200) {
            copies++;
        }

        // Stop node 0
        storage0.stop();

        // Start node 1
        storage1 = KVServiceFactory.create(port1, data1, endpoints);
        storage1.start();

        // Check node 1
        if (get(1, key, 1, 1).getStatusLine().getStatusCode() == 200) {
            copies++;
        }

        // Stop node 1
        storage1.stop();

        // Start node 2
        storage2 = KVServiceFactory.create(port2, data2, endpoints);
        storage2.start();

        // Check node 2
        if (get(2, key, 1, 1).getStatusLine().getStatusCode() == 200) {
            copies++;
        }

        // Start node 0 & 1
        storage0 = KVServiceFactory.create(port0, data0, endpoints);
        storage0.start();
        storage1 = KVServiceFactory.create(port1, data1, endpoints);
        storage1.start();

        // Check
        assertEquals(1, copies);
    }

    @Test
    public void respectRF2() throws Exception {
        final String key = randomKey();
        final byte[] value = randomValue();

        // Insert
        assertEquals(201, upsert(0, key, value, 2, 2).getStatusLine().getStatusCode());

        int copies = 0;

        // Stop all nodes
        storage0.stop();
        storage1.stop();
        storage2.stop();

        // Start node 0
        storage0 = KVServiceFactory.create(port0, data0, endpoints);
        storage0.start();

        // Check node 0
        if (get(0, key, 1, 2).getStatusLine().getStatusCode() == 200) {
            copies++;
        }

        // Stop node 0
        storage0.stop();

        // Start node 1
        storage1 = KVServiceFactory.create(port1, data1, endpoints);
        storage1.start();

        // Check node 1
        if (get(1, key, 1, 2).getStatusLine().getStatusCode() == 200) {
            copies++;
        }

        // Stop node 1
        storage1.stop();

        // Start node 2
        storage2 = KVServiceFactory.create(port2, data2, endpoints);
        storage2.start();

        // Check node 2
        if (get(2, key, 1, 2).getStatusLine().getStatusCode() == 200) {
            copies++;
        }

        // Start node 0 & 1
        storage0 = KVServiceFactory.create(port0, data0, endpoints);
        storage0.start();
        storage1 = KVServiceFactory.create(port1, data1, endpoints);
        storage1.start();

        // Check
        assertEquals(2, copies);
    }
}
