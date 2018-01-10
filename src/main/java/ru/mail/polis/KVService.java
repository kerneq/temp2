package ru.mail.polis;

/**
 * A persistent storage with HTTP API.
 * <p>
 * The following HTTP protocol is supported:
 * <ul>
 * <li>{@code GET /v0/status} -- returns {@code 200} or {@code 503}</li>
 * <li>{@code GET /v0/entity?id=<ID>[&replicas=<RF>]} -- get data by {@code ID}. Returns {@code 200} and data if found, {@code 404} if not found or {@code 504} if RF not reached.</li>
 * <li>{@code PUT /v0/entity?id=<ID>[&replicas=<RF>]} -- upsert (create or replace) data by {@code ID}. Returns {@code 201} or {@code 504} if RF not reached.</li>
 * <li>{@code DELETE /v0/entity?id=<ID>[&replicas=<RF>]} -- remove data by {@code ID}. Returns {@code 202} or {@code 504} if RF not reached.</li>
 * </ul>
 * <p>
 * {@code ID} is a non empty char sequence.
 * {@code RF} is an optional number of replicas (replica factor) to get ACKs from to return success. Default value is <b>quorum</b>.
 * <p>
 * In all the cases the storage may return:
 * <ul>
 * <li>{@code 4xx} for malformed requests</li>
 * <li>{@code 5xx} for internal errors</li>
 * </ul>
 * <p>
 * A cluster of nodes <b>MUST</b> distribute data between nodes and provide consistent results while a quorum of nodes is alive.
 *
 * @link https://github.com/polis-mail-ru/2017-highload-kv/blob/master/README.md
 * @author Vadim Tsesko <mail@incubos.org>
 */
public interface KVService {
    /**
     * Bind storage to HTTP port and start listening.
     * <p>
     * May be called only once.
     */
    void start();

    /**
     * Stop listening and free all the resources.
     * <p>
     * May be called only once and after {@link #start()}.
     */
    void stop();
}
