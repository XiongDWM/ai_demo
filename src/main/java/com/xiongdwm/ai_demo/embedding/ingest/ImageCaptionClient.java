package com.xiongdwm.ai_demo.embedding.ingest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * 简单图片 caption 客户端，支持 base64 JSON 或 multipart/form-data 上传
 */
@Component
public class ImageCaptionClient {
    private final RestTemplate rest = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${image.caption.service.url:http://localhost:9000/caption}")
    private String captionServiceUrl;

    @Value("${image.caption.useMultipart:false}")
    private boolean useMultipart;

    public String captionImage(File imageFile) {
        if (imageFile == null || !imageFile.exists()) return "";
        try {
            if (useMultipart) return postMultipart(imageFile);
            else return postBase64(imageFile);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private String postBase64(File imageFile) throws IOException {
        byte[] data = Files.readAllBytes(imageFile.toPath());
        String base64 = Base64.getEncoder().encodeToString(data);
        String payload = mapper.writeValueAsString(java.util.Map.of("image_base64", base64));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> req = new HttpEntity<>(payload, headers);
        ResponseEntity<String> resp = rest.postForEntity(captionServiceUrl, req, String.class);
        return extractCaption(resp);
    }

    private String postMultipart(File imageFile) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(imageFile));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, Object>> req = new HttpEntity<>(body, headers);
        ResponseEntity<String> resp = rest.postForEntity(captionServiceUrl, req, String.class);
        return extractCaption(resp);
    }

    private String extractCaption(ResponseEntity<String> resp) {
        if (resp == null || resp.getStatusCodeValue() / 100 != 2 || resp.getBody() == null) return "";
        String body = resp.getBody();
        try {
            JsonNode root = mapper.readTree(body);
            if (root.has("caption")) return root.get("caption").asText("");
            if (root.has("result") && root.get("result").has("caption")) return root.get("result").get("caption").asText("");
        } catch (Exception ignore) {}
        return body.trim();
    }
}

