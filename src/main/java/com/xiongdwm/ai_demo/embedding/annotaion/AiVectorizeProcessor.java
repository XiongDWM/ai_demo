package com.xiongdwm.ai_demo.embedding.annotaion;

import java.lang.reflect.Field;
import java.util.Map;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.neo4j.Neo4jVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import com.xiongdwm.ai_demo.utils.config.Neo4jVectorStoreFactory;
import com.xiongdwm.ai_demo.webapp.entities.Fiber;
import com.xiongdwm.ai_demo.webapp.entities.RoutePoint;

import jakarta.annotation.PostConstruct;

@Component
public class AiVectorizeProcessor implements BeanPostProcessor {
    @Autowired
    private Neo4jVectorStoreFactory vectorStoreFactory;
    @Autowired
    private EmbeddingModel embeddingModel;

    private static final Map<Class<?>, String> JAVA_TO_DB_TYPE = Map.of(
            Long.class, "bigint",
            Integer.class, "int",
            Double.class, "double",
            Float.class, "float",
            String.class, "varchar",
            java.util.Date.class, "datetime",
            Boolean.class, "tinyint"
    );

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (bean.getClass().isAnnotationPresent(AiVectorize.class)) {
            AiVectorize entityAnnotation = bean.getClass().getAnnotation(AiVectorize.class);
            String description = generateFullDescription(bean.getClass(), entityAnnotation);
            System.out.println(description);
        }
        return bean;
    }

    @PostConstruct
    public void printAllEntityDescriptions() {
        VectorStore v = vectorStoreFactory.createVectorStore("db_description", "db_description", embeddingModel);
        Class<?>[] entities = { Fiber.class, RoutePoint.class };
        for (Class<?> clazz : entities) {
            AiVectorize entityAnnotation = clazz.getAnnotation(AiVectorize.class);
            if (entityAnnotation != null) {
                String description = generateFullDescription(clazz, entityAnnotation);
                // v.delete("'id'>0");
                // v.add(List.of(new Document(description, Map.of("subdivision", "db_description"))));
                System.out.println(description);
            }
        }
    }

    private String generateFullDescription(Class<?> clazz, AiVectorize entityAnnotation) {
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
        AiVectorize entityAnnotation = Fiber.class.getAnnotation(AiVectorize.class);
        if (entityAnnotation != null) {
            String description = new AiVectorizeProcessor().generateFullDescription(Fiber.class, entityAnnotation);
            System.out.println(description);
        } else {
            System.out.println("No AiVectorize annotation found on Fiber.");
        }
    }
}