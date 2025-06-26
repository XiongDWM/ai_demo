package com.xiongdwm.ai_demo.chat;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.xiongdwm.ai_demo.utils.JacksonUtil;
import com.xiongdwm.ai_demo.utils.cache.CacheHandler;
import com.xiongdwm.ai_demo.utils.cache.LRUCache;

import jakarta.annotation.Resource;
import reactor.core.publisher.FluxSink;

@Component
public class ChatContextManager {
    @Value("${chat.context.length}")
    private int contextSize;

    @Resource
    private CacheHandler cacheHandler;

    private final Map<String, FluxSink<String>> sinkMap = new java.util.concurrent.ConcurrentHashMap<>();

    public void putContextToCache(String sessionId, String question, String answer) {
        cacheHandler.setCertainValueToCache(sessionId, question, answer);
    }

    public List<String> getAllContextFromCache(String sessionId) {
        LRUCache<String, String> cache = cacheHandler.getCache(sessionId, contextSize, 10 * 60 * 1000);
        if (cache.isEmpty()) {
            return Collections.emptyList();
        }
        List<Map.Entry<String, String>> context = cache.getAllKV();
        List<String> contextInQAFormat = context.stream()
                .map(entry -> "问题：" + entry.getKey() + "\n" + "回答：" + entry.getValue() + "\n")
                .toList();
        return contextInQAFormat;
    }

    public void registerSink(String conversationId, FluxSink<String> sink) {
        sinkMap.put(conversationId, sink);
    }

    public void removeSink(String conversationId) {
        sinkMap.remove(conversationId);
    }

    public void cancel(String conversationId, String message) {
        System.out.println("cvid" + conversationId);
        var sink = sinkMap.get(conversationId);
        System.out.println(sinkMap);
        if (sink != null) {
            sink.next(JacksonUtil.toJsonString(new ConversationContext(message, conversationId)).get());
            System.out.println("=====================================================================>>");
            sink.complete();
            sinkMap.remove(conversationId);
        }
    }

    public void clearCacheForSession(String sessionId) {
        cacheHandler.removeCache(sessionId);
    }

    public String getLatest(String sessionId) {
        LRUCache<String, String> cache = cacheHandler.getCache(sessionId, contextSize, 10 * 60 * 1000);
        if (cache.isEmpty()) {
            return null;
        }
        var context = cache.peek();
        if (null == context) {
            return null;
        }
        String question = context.getKey();
        String answer = context.getValue();
        return "问题：" + question + "\n" + "回答：" + answer + "\n";
    }

    public void print() {
        System.out.println("缓存信息：" + cacheHandler.toString());
        System.out.println("当前上下文大小：" + contextSize);
    }

    public List<String> getLatestWithIntents(String chatId) {
        var intents = List.of(IntentsEnum.values());
        System.out.println("意图有: " + intents);
        System.out.println("topic: " + chatId);
        System.out.println(cacheHandler.toString());
        return intents.parallelStream().map(intent -> {
            System.out.println("new topic: " + chatId + "-" + intent.getCode());
            LRUCache<String, String> cache = cacheHandler.getCache(chatId + "-" + intent.getCode());
            if (cache.isEmpty()) {
                System.out.println(chatId + "-" + intent.getCode() + " is empty");
                return null;
            }
            var context = cache.peek();
            System.out.println("cache:" + chatId + "-" + intent.getCode() + "->" + context);
            if (null == context) {
                return null;
            }
            String question = context.getKey();
            String answer = context.getValue();
            System.out.println(
                    "###意图【" + intent.getName() + "】\n" + "###问题：" + question + "\n" + "###回答：" + answer + "\n");
            return "###意图【" + intent.getName() + "】\n" + "###问题：" + question + "\n " + "###回答：" + answer + "\n";
        }).filter(result -> result != null).toList();
    }

}
