package com.xiongdwm.ai_demo.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.IOException;
import java.util.*;

public class JacksonUtil {
    public static class Either{
        boolean array;
        JsonNode single;
        List<JsonNode> nodeList;

        public boolean isArray() {
            return array;
        }

        public JsonNode getSingle() {
            return single;
        }

        public List<JsonNode> getNodeList() {
            return nodeList;
        }
    }
    static ObjectMapper objectMapper=new ObjectMapper();

    public static String parse(Object obj){
        try {
            return objectMapper.writeValueAsString(obj);
        }catch (JsonProcessingException e){
            System.out.println(e.getLocalizedMessage());
        }
        return "";
    }

    public static<T> T parse(String str,Class<T> clazz){
        try{
            return objectMapper.readValue(str,clazz);
        }catch (IOException e){
            System.out.println(e.getLocalizedMessage());
        }
        return null;
    }

    public static<T> Optional<T> map2Object(Map<?,?>map,Class<T> clazz){
        try {
            return Optional.of(objectMapper.convertValue(map, clazz));
        }catch (IllegalArgumentException e){
            System.out.println(e.getLocalizedMessage());
        }
        return Optional.empty();
    }


    public static ObjectNode mapToNode(Map<String,Object>map){
        return objectMapper.valueToTree(map);
    }

    public static <T> List<T> fromJsonToList(String json, Class<T> clazz) throws IOException {
        return objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(List.class, clazz));
    }

    public static <T> List<T> fromJsonToList(String json, TypeReference<List<T>> typeReference) throws IOException {
        return objectMapper.readValue(json, typeReference);
    }

    public static<K,V> ArrayNode mapToKVArrayNode(Map<K,V>map){
        ArrayNode arrayNode=objectMapper.createArrayNode();
        map.forEach((key, value) -> {
            ObjectNode obj = objectMapper.createObjectNode();
            obj.set("key", objectMapper.valueToTree(key));
            obj.set("value", objectMapper.valueToTree(value));
            arrayNode.add(obj);
        });
        return arrayNode;
    }

    public static<K,V> ArrayNode mapToKVArrayNode(Map<K,V>map,String customizeKeyName,String customizeValueName){
        ArrayNode arrayNode=objectMapper.createArrayNode();
        map.forEach((key, value) -> {
            ObjectNode obj = objectMapper.createObjectNode();
            obj.set(customizeKeyName, objectMapper.valueToTree(key));
            obj.set(customizeValueName, objectMapper.valueToTree(value));
            arrayNode.add(obj);
        });
        return arrayNode;
    }



    public static Either convertJsonStringToJsonNode(String jsonString){
        JsonNode jsonNode;
        try {
            jsonNode = objectMapper.readTree(jsonString);
        } catch (IOException e) {
            System.out.println(e.getLocalizedMessage());
            return null;
        }
        Either e=new Either();
        e.array=jsonNode.isArray();
        if(jsonNode.isArray()){
            List<JsonNode> nodes=new ArrayList<>();
            Iterator<JsonNode> iterator=jsonNode.elements();
            while(iterator.hasNext())nodes.add(iterator.next());
            e.nodeList=nodes;
        }else{
            e.single=jsonNode;
        }
        return e;
    }

        public static JsonNode convertToJsonNode(Object obj){
        String jsonString=parse(obj);
        JsonNode jsonNode = null;
        try {
            jsonNode = objectMapper.readTree(jsonString);
        } catch (IOException e) {
            System.out.println(e.getLocalizedMessage());
        }
        return jsonNode;
    }

}
