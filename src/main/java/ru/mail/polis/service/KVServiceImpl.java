package ru.mail.polis.service;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.mail.polis.KVService;
import ru.mail.polis.http.HttpMethod;
import ru.mail.polis.http.ParseQuery;
import ru.mail.polis.http.QueryParams;
import ru.mail.polis.http.Response;
import ru.mail.polis.storage.Storage;
import ru.mail.polis.util.DataReader;
import ru.mail.polis.util.GetServers;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletionService;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;

import static ru.mail.polis.http.HttpMethod.DELETE;
import static ru.mail.polis.http.HttpMethod.GET;
import static ru.mail.polis.http.HttpMethod.PUT;
import static ru.mail.polis.http.Response.*;

public class KVServiceImpl implements KVService {

    private static final String URL_STATUS = "/v0/status";
    private static final String URL_INNER = "/v0/inner";
    private static final String URL_ENTITY = "/v0/entity";
    private static final String URL_SERVER = "http://localhost";

    private static final String METHOD_IS_NOT_ALLOWED = "Method is not allowed";

    @NotNull
    private final HttpServer server;
    @NotNull
    private final Storage dao;
    @NotNull
    private final List<String> topology;
    @NotNull
    private final CompletionService<Response> completionService;

    public KVServiceImpl(int port,
                         @NotNull Storage dao,
                         @NotNull Set<String> topology) throws IOException {
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.dao = dao;
        this.topology = new ArrayList<>(topology);
        Executor executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        this.completionService = new ExecutorCompletionService<>(executor);

        server.createContext(URL_STATUS, this::processStatus);
        server.createContext(URL_INNER, this::processInner);
        server.createContext(URL_ENTITY, this::processEntity);
    }

    @Override
    public void start() {
        server.start();
    }

    @Override
    public void stop() {
        server.stop(0);
    }

    private void processStatus(@NotNull HttpExchange http) throws IOException {
        sendResponse(http, new Response(OK));
    }

    private void processInner(@NotNull HttpExchange http) throws IOException {
        try {
            QueryParams params = ParseQuery.parseQuery(http.getRequestURI().getQuery(), topology);

            Response resp;
            switch (HttpMethod.valueOf(http.getRequestMethod())) {
                case GET:
                    resp = processInnerGet(params);
                    break;
                case PUT:
                    final byte[] data = DataReader.readData(http.getRequestBody());
                    resp = processInnerPut(params, data);
                    break;
                case DELETE:
                    resp = processInnerDelete(params);
                    break;
                default:
                    resp = new Response(NOT_ALLOWED, METHOD_IS_NOT_ALLOWED);
                    break;
            }
            sendResponse(http, resp);
        } catch (IllegalArgumentException e) {
            sendResponse(http, new Response(BAD_REQUEST, e.getMessage()));
        }
    }

    private Response processInnerGet(@NotNull QueryParams params) throws IOException {
        try {
            String id = params.getId();
            final byte[] getValue = dao.get(id);
            return new Response(OK, getValue);
        } catch (IllegalArgumentException e) {
            return new Response(BAD_REQUEST, e.getMessage());
        } catch (NoSuchElementException e) {
            return new Response(NOT_FOUND, e.getMessage());
        }
    }

    private Response processInnerPut(@NotNull QueryParams params,
                                     @NotNull byte[] data) {
        try {
            String id = params.getId();
            dao.upsert(id, data);
            return new Response(CREATED);
        } catch (IOException | IllegalArgumentException e) {
            return new Response(BAD_REQUEST, e.getMessage());
        }
    }

    private Response processInnerDelete(@NotNull QueryParams params) throws IOException {
        try {
            String id = params.getId();
            dao.delete(id);
            return new Response(ACCEPTED);
        } catch (IllegalArgumentException e) {
            return new Response(BAD_REQUEST, e.getMessage());
        }
    }

    private void processEntity(@NotNull HttpExchange http) throws IOException {
        try {
            QueryParams params = ParseQuery.parseQuery(http.getRequestURI().getQuery(), topology);

            Response resp;
            switch (HttpMethod.valueOf(http.getRequestMethod())) {
                case GET:
                    resp = processEntityGet(params);
                    break;
                case PUT:
                    final byte[] data = DataReader.readData(http.getRequestBody());
                    resp = processEntityPut(params, data);
                    break;
                case DELETE:
                    resp = processEntityDelete(params);
                    break;
                default:
                    resp = new Response(NOT_ALLOWED, METHOD_IS_NOT_ALLOWED);
                    break;
            }
            sendResponse(http, resp);
        } catch (IllegalArgumentException e) {
            sendResponse(http, new Response(BAD_REQUEST, e.getMessage()));
        }
    }

    private Response processEntityGet(@NotNull QueryParams params) throws IOException {
        try {
            String id = params.getId();
            List<String> nodes = GetServers.getNodesById(id, params.getFrom(), topology);
            executeFutures(GET, params, nodes, null);

            int ok = 0;
            int notFound = 0;
            byte[] value = null;
            for (int i = 0; i < params.getFrom(); i++) {
                try {
                    Response resp = completionService.take().get();
                    if (resp.getCode() == OK) {
                        ok++;
                        value = resp.getData();
                    } else if (resp.getCode() == NOT_FOUND) {
                        notFound++;
                    }
                } catch (Exception e) {
                    return new Response(SERVER_ERROR);
                }
            }

            boolean hasLocally = processInnerGet(params).getCode() == OK;
            if (ok > 0 && notFound == 1 && !hasLocally) {
                // we probably missed the insertion
                processInnerPut(params, value);
                notFound--;
                ok++;
            }

            if (ok + notFound < params.getAck()) {
                return new Response(NOT_ENOUGH_REPLICAS);
            } else if (ok < params.getAck()) {
                return new Response(NOT_FOUND);
            } else {
                return new Response(OK, value);
            }
        } catch (IllegalArgumentException e) {
            return new Response(BAD_REQUEST, e.getMessage());
        } catch (NoSuchElementException e) {
            return new Response(NOT_FOUND, e.getMessage());
        }
    }

    private Response processEntityPut(@NotNull QueryParams params,
                                      @Nullable byte[] data) {
        try {
            List<String> nodes = GetServers.getNodesById(params.getId(), params.getFrom(), topology);
            executeFutures(PUT, params, nodes, data);

            int ok = 0;
            for (int i = 0; i < params.getFrom() && ok < params.getAck(); i++) {
                try {
                    Response resp = completionService.take().get();
                    if (resp.getCode() == CREATED) {
                        ok++;
                    }
                } catch (Exception e) {
                    return new Response(SERVER_ERROR);
                }
            }

            if (ok < params.getAck()) {
                return new Response(NOT_ENOUGH_REPLICAS);
            } else {
                return new Response(CREATED);
            }
        } catch (IllegalArgumentException e) {
            return new Response(BAD_REQUEST, e.getMessage());
        } catch (NoSuchElementException e) {
            return new Response(NOT_FOUND, e.getMessage());
        }
    }

    private Response processEntityDelete(@NotNull QueryParams params) {
        try {
            List<String> nodes = GetServers.getNodesById(params.getId(), params.getFrom(), topology);
            executeFutures(DELETE, params, nodes, null);

            int ok = 0;
            for (int i = 0; i < params.getFrom() && ok < params.getAck(); i++) {
                try {
                    Response resp = completionService.take().get();
                    if (resp.getCode() == ACCEPTED) {
                        ok++;
                    }
                } catch (Exception e) {
                    return new Response(SERVER_ERROR);
                }
            }

            if (ok < params.getAck()) {
                return new Response(NOT_ENOUGH_REPLICAS);
            } else {
                return new Response(ACCEPTED);
            }
        } catch (IllegalArgumentException e) {
            return new Response(BAD_REQUEST, e.getMessage());
        } catch (NoSuchElementException e) {
            return new Response(NOT_FOUND, e.getMessage());
        }
    }

    private void executeFutures(@NotNull HttpMethod method,
                                @NotNull QueryParams params,
                                @NotNull List<String> nodes,
                                @Nullable byte[] data) {
        String self = URL_SERVER + ":" + server.getAddress().getPort();
        for (String node : nodes) {
            if (node.equals(self)) {
                switch (method) {
                    case GET:
                        completionService.submit(() -> processInnerGet(params));
                        break;
                    case PUT:
                        completionService.submit(() -> processInnerPut(params, data));
                        break;
                    case DELETE:
                        completionService.submit(() -> processInnerDelete(params));
                        break;
                    default:
                        throw new IllegalArgumentException(METHOD_IS_NOT_ALLOWED);
                }
            } else {
                completionService.submit(
                        () -> makeRequest(method, node + URL_INNER, "?id=" + params.getId(), data));
            }
        }
    }

    private Response makeRequest(@NotNull HttpMethod method,
                                 @NotNull String link,
                                 @NotNull String params,
                                 @Nullable byte[] data) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(link + params);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method.toString());
            conn.setDoOutput(method == PUT);
            conn.connect();

            if (method == PUT) {
                conn.getOutputStream().write(data);
                conn.getOutputStream().flush();
                conn.getOutputStream().close();
            }

            int code = conn.getResponseCode();
            if (method == GET && code == OK) {
                InputStream dataStream = conn.getInputStream();
                byte[] inputData = DataReader.readData(dataStream);
                return new Response(code, inputData);
            }
            return new Response(code);
        } catch (IOException e) {
            return new Response(SERVER_ERROR);
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private void sendResponse(@NotNull HttpExchange http,
                              @NotNull Response resp) throws IOException {
        if (resp.hasData()) {
            http.sendResponseHeaders(resp.getCode(), resp.getData().length);
            http.getResponseBody().write(resp.getData());
        } else {
            http.sendResponseHeaders(resp.getCode(), 0);
        }
        http.close();
    }
}
