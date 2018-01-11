package ru.mail.polis.http;

import org.jetbrains.annotations.NotNull;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by iters on 1/11/18.
 */
public class ParseQuery {

    private static final String QUERY_ID = "id";
    private static final String QUERY_REPLICAS = "replicas";
    private static final Map<String, QueryParams> cache;

    static {
        cache = new HashMap<>();
    }

    public static QueryParams parseQuery(@NotNull String query,
                                         @NotNull List<String> topology) {
        if (cache.containsKey(query)) {
            return cache.get(query);
        }

        Map<String, String> params = parseParams(query);
        String id = params.get(QUERY_ID);
        int ack;
        int from;
        if (params.containsKey(QUERY_REPLICAS)) {
            String replicasParams[] = params.get(QUERY_REPLICAS).split("/");
            ack = Integer.valueOf(replicasParams[0]);
            from = Integer.valueOf(replicasParams[1]);
        } else {
            ack = topology.size() / 2 + 1;
            from = topology.size();
        }

        if (id == null || "".equals(id) || ack < 1 || from < 1 || ack > from) {
            throw new IllegalArgumentException("Query is invalid");
        }

        QueryParams par  = new QueryParams(id, ack, from);
        cache.put(id, par);
        return par;
    }

    private static Map<String, String> parseParams(@NotNull String query) {
        try {
            Map<String, String> params = new LinkedHashMap<>();
            for (String param : query.split("&")) {
                int idx = param.indexOf("=");
                params.put(URLDecoder.decode(param.substring(0, idx), "UTF-8"),
                        URLDecoder.decode(param.substring(idx + 1), "UTF-8"));
            }
            return params;
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("Query is invalid");
        }
    }
}
