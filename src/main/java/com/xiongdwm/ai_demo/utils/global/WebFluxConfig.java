package com.xiongdwm.ai_demo.utils.global;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.ResourceHandlerRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@Configuration
public class WebFluxConfig implements WebFluxConfigurer{

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 将 upload/images 映射到 /static/images/** 便于外部访问
        registry.addResourceHandler("/static/images/**")
                .addResourceLocations("file:upload/images/");
    }
}
