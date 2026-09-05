package com.wallet.security.testutil;

import com.wallet.security.spi.PaySecurityKeyValueStore;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 带 TTL 与 setIfAbsent 语义的内存 KV，模拟 Redis 行为供单元测试使用。
 */
public class InMemoryKvStore implements PaySecurityKeyValueStore {

    private static final class Entry {
        String value;
        long expiresAt;

        Entry(String value, long expiresAt) {
            this.value = value;
            this.expiresAt = expiresAt;
        }
    }

    private final Map<String, Entry> store = new ConcurrentHashMap<>();
    private boolean failSet;

    /** 让后续 set 全部失败，模拟存储不可用。 */
    public void failNextSets(boolean fail) {
        this.failSet = fail;
    }

    public boolean exists(String key) {
        return live(key) != null;
    }

    @Override
    public String get(String key) {
        Entry entry = live(key);
        return entry == null ? null : entry.value;
    }

    @Override
    public boolean set(String key, String value, long ttlSeconds) {
        if (failSet) {
            return false;
        }
        store.put(key, new Entry(value, ttlSeconds > 0 ? System.currentTimeMillis() + ttlSeconds * 1000 : 0));
        return true;
    }

    @Override
    public boolean setIfAbsent(String key, String value, long ttlSeconds) {
        if (live(key) != null) {
            return false;
        }
        return set(key, value, ttlSeconds);
    }

    @Override
    public boolean delete(String key) {
        Entry entry = live(key);
        if (entry == null) {
            return false;
        }
        return store.remove(key) != null;
    }

    @Override
    public long increment(String key, long delta, long ttlSeconds) {
        synchronized (store) {
            Entry entry = live(key);
            long value = entry == null ? 0L : Long.parseLong(entry.value);
            value += delta;
            long expiresAt = ttlSeconds > 0 ? System.currentTimeMillis() + ttlSeconds * 1000 : 0;
            if (entry == null) {
                store.put(key, new Entry(String.valueOf(value), expiresAt));
            } else {
                entry.value = String.valueOf(value);
                entry.expiresAt = expiresAt;
            }
            return value;
        }
    }

    private Entry live(String key) {
        Entry entry = store.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.expiresAt > 0 && entry.expiresAt <= System.currentTimeMillis()) {
            store.remove(key);
            return null;
        }
        return entry;
    }
}
