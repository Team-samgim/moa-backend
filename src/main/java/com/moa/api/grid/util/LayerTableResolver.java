package com.moa.api.grid.util;

import java.util.Locale;
import java.util.Map;

public final class LayerTableResolver {

    private LayerTableResolver() {}

    private static final Map<String, String> MAP = Map.of(
            "HTTP_PAGE", "public.http_page_sample",
            "HTTP_URI",  "public.http_uri_sample",
            "L4_TCP",    "public.l4_tcp_sample",
            "ETHERNET",  "public.ethernet_sample"
    );

    public static String resolveDataTable(String layer) {
        if (layer == null) return "public.http_page_sample";
        String key = layer.trim().toUpperCase();
        return MAP.getOrDefault(key, "public.http_page_sample");
    }
}