package com.xiongdwm.ai_demo.utils.cache;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class LRUCache<K, V> {
    private static class CacheEntry<K, V> implements Comparable<CacheEntry<K, V>> {
        K key;
        V value;
        long expireTime;

        public CacheEntry(K key, V value, long expireTime) {
            this.key = key;
            this.value = value;
            this.expireTime = expireTime;
        }

        @Override
        public int compareTo(CacheEntry<K, V> other) {
            return Long.compare(this.expireTime, other.expireTime);
        }

        @Override
        public String toString() {
            return "Entry{" +
                    "key=" + key +
                    ", value=" + value +
                    ", expireTime=" + expireTime +
                    '}';
        }
    }

    private final int capacity;
    private final long expireTimeLimit;
    private final ConcurrentHashMap<K, CacheEntry<K, V>> map;
    private final PriorityQueue<CacheEntry<K, V>> queue;
    private final ReentrantLock lock = new ReentrantLock();
    private static final long DEFAULT_EXPIRE_TIME = 5 * 60 * 1000; // default expire time is 5 minutes
    private static final int DEFAULT_CAPACITY = 64; // default capacity is 32
    private final ScheduledExecutorService scheduler;
    private volatile K latest;

    // constructors
    public LRUCache() {
        this(DEFAULT_CAPACITY, DEFAULT_EXPIRE_TIME);
    }

    public LRUCache(int capacity) {
        this(capacity, DEFAULT_EXPIRE_TIME);
    }

    public LRUCache(long expireTime) {
        this(DEFAULT_CAPACITY, expireTime);
    }

    public LRUCache(int capacity, long expireTimeLimit) {
        this.capacity = capacity;
        this.expireTimeLimit = expireTimeLimit;
        this.map = new ConcurrentHashMap<>();
        this.queue = new PriorityQueue<>();

        this.scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::clearExpiredEntries, expireTimeLimit, expireTimeLimit,
                TimeUnit.MILLISECONDS);
    }

    public V get(K key) {
        lock.lock();
        try {
            CacheEntry<K, V> entry = map.get(key);
            if (entry == null || System.currentTimeMillis() >= entry.expireTime) {
                return null;
            }
            entry.expireTime = System.currentTimeMillis() + expireTimeLimit;
            queue.remove(entry);
            queue.offer(entry);
            return entry.value;
        } finally {
            lock.unlock();
        }
    }

    public void put(K key, V value) {
        lock.lock();
        try {
            long currentTime = System.currentTimeMillis();
            CacheEntry<K, V> entry = new CacheEntry<>(key, value, currentTime + expireTimeLimit);
            if (map.containsKey(key)) {
                CacheEntry<K, V> oldEntry = map.get(key);
                map.remove(key);
                queue.remove(oldEntry);
            }
            if (map.size() >= capacity) {
                CacheEntry<K, V> eldestEntry = queue.poll();
                if (eldestEntry != null) {
                    map.remove(eldestEntry.key);
                }
            }
            map.put(key, entry);
            latest = key;
            queue.offer(entry);
        } finally {
            lock.unlock();
        }
    }

    public Map.Entry<K,V> peek() {
        lock.lock();
        try {
            if (latest == null)
                return null;
            CacheEntry<K, V> entry = map.get(latest);
            return entry == null ? null : new AbstractMap.SimpleEntry<>(entry.key, entry.value);
        } finally {
            lock.unlock();
        }
    }

    public List<V> getAllValues() {
        lock.lock();
        try {
            if (isEmpty())
                return Collections.emptyList();
            List<V> values = new ArrayList<>();
            map.forEach((key, entry) -> values.add(entry.value));
            return values;
        } finally {
            lock.unlock();
        }
    }

    public List<Map.Entry<K, V>> getAllKV() {
        lock.lock();
        try {
            if (isEmpty()) return Collections.emptyList();
            List<Map.Entry<K, V>> entries = new ArrayList<>();
            map.forEach((key, entry) -> entries.add(new AbstractMap.SimpleEntry<>(entry.key, entry.value)));
            return entries;
        } finally {
            lock.unlock();
        }
    }

    private void clearExpiredEntries() {
        lock.lock();
        try {
            Iterator<CacheEntry<K, V>> iterator = queue.iterator();
            while (iterator.hasNext()) {
                CacheEntry<K, V> expiredEntry = iterator.next();
                if (expiredEntry != null && System.currentTimeMillis() >= expiredEntry.expireTime) {
                    iterator.remove();
                    map.remove(expiredEntry.key);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public boolean isFull() {
        return map.size() >= capacity;
    }

    public ConcurrentHashMap<K, CacheEntry<K, V>> getMap() {
        return map;
    }

    public PriorityQueue<CacheEntry<K, V>> getQueue() {
        return queue;
    }

    // shutdown method to clear the cache and stop the scheduler
    public void shutdown() {
        lock.lock();
        try {
            queue.clear();
            map.clear();
            scheduler.shutdown();
        } finally {
            lock.unlock();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        LRUCache<Integer, Integer> cache = new LRUCache<>(2, 1000 * 40);
        cache.put(1, 1);
        cache.put(2, 2);
        System.out.println(cache.map.values());

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        scheduler.schedule(() -> {
            Integer v1 = cache.get(1);
            System.out.println("================visit entry 1::" + v1 + "==================");
        }, 30, TimeUnit.SECONDS);
        scheduler.shutdown();

        int capacity = 2;
        LinkedHashMap<String, String> s = new LinkedHashMap<String, String>(capacity, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                return size() > capacity;
            }
        };
        s.put("a", "a");
        s.put("b", "b");
        System.out.println(s.values());
        System.out.println(s.get("b"));
        System.out.println(s.values());
        s.put("c", "c");
        System.out.println(s);
    }

}
