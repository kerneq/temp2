package ru.mail.polis.util;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by iters on 1/11/18.
 */
public class GetServers {

    private static final Map<String, List<String>> cache;

    static {
        cache = new HashMap<>();
    }

    public static List<String> getNodesById(@NotNull String id,
                                            int from,
                                            @NotNull List<String> topology) {
        if (cache.containsKey(id + from)) {
            return cache.get(topology + id + from);
        }

        List<String> nodes = new ArrayList<>();
        int hash = Math.abs(id.hashCode());
        for (int i = 0; i < from; i++) {
            int idx = (hash + i) % topology.size();
            nodes.add(topology.get(idx));
        }

        cache.put(topology + id + from, nodes);
        return nodes;
    }
}
