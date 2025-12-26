package com.xiongdwm.ai_demo.webapp.resource;

import com.xiongdwm.ai_demo.utils.global.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;

@RestController
public class ImageController {

    @Value("${pic.doc.path.cs}")
    private String imageRoot;


    @GetMapping("/getImage/param")
    public Mono<ResponseEntity<Resource>> getImageByPathParam(@RequestParam("path") String p){
        if (!StringUtils.hasText(p)) return Mono.just(ResponseEntity.badRequest().build());
        var picId=Arrays.stream(p.split("/")).toList().getLast();
        try {
            Path root = Paths.get(imageRoot).toAbsolutePath().normalize();
            Path file = Paths.get(imageRoot+File.separator+picId).toAbsolutePath().normalize();
            if (!file.startsWith(root)) return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
            File f = file.toFile();
            if (!f.exists() || !f.isFile()) return Mono.just(ResponseEntity.notFound().build());

            String contentType = Files.probeContentType(file);
            if (contentType == null) contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;

            String etag = "\"" + f.lastModified() + "-" + f.length() + "\"";

            return Mono.just(ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
                    .eTag(etag)
                    .body(new FileSystemResource(f)));
        } catch (Exception e) {
            return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
        }
    }

}
