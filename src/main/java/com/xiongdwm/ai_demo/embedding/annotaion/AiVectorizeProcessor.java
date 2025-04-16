package com.xiongdwm.ai_demo.embedding.annotaion;

import java.lang.reflect.Field;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import com.xiongdwm.ai_demo.webapp.entities.TestEntity;


@Component
public class AiVectorizeProcessor implements BeanPostProcessor {
    // This class is a placeholder for the actual implementation of the vectorization process.
    // The actual implementation would involve using the annotations to generate vectors for the entities.
    // For now, it just serves as a marker to indicate where the vectorization logic would go.

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {

        return bean;
    }

    private String generateFullDescription(Class<?> clazz, AiVectorize entityAnnotation){
        StringBuilder description = new StringBuilder();
        description.append("###").append(entityAnnotation.name()).append("\n")
                   .append(entityAnnotation.description())
                   .append("\n");

        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(AiVectorize.class)) {
                AiVectorize fieldAnnotation = field.getAnnotation(AiVectorize.class);
                description.append(" - ").append(fieldAnnotation.name()).append(": ")
                            .append(fieldAnnotation.description()).append("\n");
            }
        }

        return description.toString();
    }
    public static void main(String[] args) {
        AiVectorize entityAnnotation = TestEntity.class.getAnnotation(AiVectorize.class);
        if (entityAnnotation != null) {
            String description = new AiVectorizeProcessor().generateFullDescription(TestEntity.class, entityAnnotation);
            System.out.println(description);
        } else {
            System.out.println("No AiVectorize annotation found on TestEntity.");
        }
    }
}