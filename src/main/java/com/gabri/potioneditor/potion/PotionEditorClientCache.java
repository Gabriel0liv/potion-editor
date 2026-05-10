package com.gabri.potioneditor.potion;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class PotionEditorClientCache {
    private static final Map<String, PotionVariantData> OVERRIDES = new ConcurrentHashMap<>();
    private static final AtomicLong VERSION = new AtomicLong();

    private PotionEditorClientCache() {
    }

    public static Optional<PotionVariantData> get(PotionVariantKey key) {
        PotionVariantData data = OVERRIDES.get(key.asStringKey());
        return data == null ? Optional.empty() : Optional.of(data.copy());
    }

    public static void put(PotionVariantKey key, PotionVariantData data) {
        if (key == null || data == null || data.isEmpty()) {
            return;
        }
        OVERRIDES.put(key.asStringKey(), data.copy());
        VERSION.incrementAndGet();
    }

    public static void remove(PotionVariantKey key) {
        if (key != null) {
            OVERRIDES.remove(key.asStringKey());
            VERSION.incrementAndGet();
        }
    }

    public static void replaceAll(Map<String, PotionVariantData> entries) {
        OVERRIDES.clear();
        if (entries == null) {
            return;
        }
        for (Map.Entry<String, PotionVariantData> entry : entries.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                OVERRIDES.put(entry.getKey(), entry.getValue().copy());
            }
        }
        VERSION.incrementAndGet();
    }

    public static long version() {
        return VERSION.get();
    }
}

