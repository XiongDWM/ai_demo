package com.xiongdwm.ai_demo.embedding.annotaion;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.FIELD})
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface AiVectorize {
    String name() default "";
    String description() default "";
    AiVectorizeType type() default AiVectorizeType.UNKNOWN;

    public enum AiVectorizeType {
        ENTITY("entity_description"),
        FIELDS("entity_description"),
        SERVICE("question_to_query"),
        QUERY("question_to_query"),
        UNKNOWN("NONE");
    
        private final String label;
        AiVectorizeType(String label) {
            this.label = label;
        }
        public String getLabel() {
            return label;
        }
    
    }

}
