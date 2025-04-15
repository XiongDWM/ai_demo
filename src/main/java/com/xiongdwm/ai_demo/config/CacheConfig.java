package com.xiongdwm.ai_demo.config;



import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.xiongdwm.ai_demo.utils.cache.CacheHandler;

@Configuration
public class CacheConfig {
    
    @Bean
    public CacheHandler cacheHandler() {
        System.out.println("===========cache handler init================>>>>");
        return new CacheHandler();
    }
}
