package com.xiongdwm.ai_demo.utils.cache;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Component;

@Component
public class CacheHandler {
    private final ConcurrentMap<String, LRUCache<?, ?>> caches = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public <K,V> LRUCache<K, V> getCache(String name) {
        return (LRUCache<K,V>)caches.computeIfAbsent(name, k -> new LRUCache<>());
    }

    @SuppressWarnings("unchecked")
    public<K,V> LRUCache<K, V> getCache(String name, int capacity) {
        return (LRUCache<K,V>)caches.computeIfAbsent(name, k -> new LRUCache<>(capacity));
    }

    @SuppressWarnings("unchecked")
    public<K,V> LRUCache<K, V> getCache(String name, long expireTime) {
        return (LRUCache<K,V>)caches.computeIfAbsent(name, k -> new LRUCache<>(expireTime));
    }

    
    @SuppressWarnings("unchecked")
    public<K,V> LRUCache<K, V> getCache(String name, int capacity, long expireTime) {
        return (LRUCache<K,V>)caches.computeIfAbsent(name, k -> new LRUCache<>(capacity, expireTime));
    }

    public boolean isEmptyWithinKey(String name){
        LRUCache<?, ?> cache = caches.get(name);
        return null == cache || cache.isEmpty();
    }

    public void removeCache(String name) {
        LRUCache<?,?>cache=caches.remove(name);
        if(null!=cache){
            cache.shutdown();;
        }
    }

    public <K,V> void addCache(String name, LRUCache<K, V> cache) {
        caches.put(name, cache);
    }

    public<K,V> void setCertainValueToCache(String name, K key, V value) {
        LRUCache<K, V> cache = getCache(name);
        if(null==cache)throw new IllegalArgumentException("cache with name "+name+" not found");
        cache.put(key, value);
    }
    
    public String toString() {
        return caches.toString();
    }
}
