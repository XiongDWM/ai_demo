package com.xiongdwm.ai_demo.chat;


import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.xiongdwm.ai_demo.utils.cache.CacheHandler;
import com.xiongdwm.ai_demo.utils.cache.LRUCache;

import jakarta.annotation.Resource;

@Component
public class ChatContextManager {
    @Value("${chat.context.length}")
    private int contextSize;
    
    @Resource
    private CacheHandler cacheHandler;

    public void putContextToCache(String sessionId, String question, String answer) {
        cacheHandler.setCertainValueToCache(sessionId, question, answer);
    }

    public List<String> getAllContextFromCache(String sessionId){
        LRUCache<String, String> cache = cacheHandler.getCache(sessionId, contextSize,10* 60 * 1000);
        if(cache.isEmpty()){
            return Collections.emptyList();
        }
        List<Map.Entry<String,String>> context = cache.getAllKV();
        List<String>contextInQAFormat=
                context.stream()
                .map(entry -> "问题：" + entry.getKey() + "\n" + "回答：" + entry.getValue() + "\n")
                .toList();
        return contextInQAFormat;
    }
    
    public void clearCacheForSession(String sessionId) {
        cacheHandler.removeCache(sessionId);
    }

    public void print(){
        System.out.println("缓存信息："+cacheHandler.toString());
        System.out.println("当前上下文大小：" + contextSize);
    }

}
