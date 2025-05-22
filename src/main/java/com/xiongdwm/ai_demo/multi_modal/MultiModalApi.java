package com.xiongdwm.ai_demo.multi_modal;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.Media;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;

import com.xiongdwm.ai_demo.utils.cache.CacheHandler;
import com.xiongdwm.ai_demo.utils.cache.LRUCache;
import com.xiongdwm.ai_demo.utils.global.ApiResponse;

import org.springframework.ai.chat.model.ChatResponse;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
public class MultiModalApi {
    @Autowired
    private OllamaChatModel model;

    @Value("${file.upload.path}")
    private String uploadPath;

    @Autowired
    CacheHandler cacheHandler;
    @Autowired
    MultiModalService multiModalService;

    @GetMapping(value = "/mm/picture/ocr")
    public String extractTextFromImage(@RequestParam("name") String name) {

        String imagePath = uploadPath + File.separator + name;
        FileSystemResource resource = new FileSystemResource(imagePath);
        var text = "##The user provides a picture and you need to extract the text in the picture. You will only respond with Chinese and English characters.\n"
                +
                "##The text in the picture formatted from left to right and in multiple lines.\n" +
                "##You must not classify them by language.\n" +
                "##You must extract each character in the original order.\n" +
                "##Ignore all line breaks in the extracted text and respond with a single continuous string.\n" +
                "##Do not answer with any explanation, imagination or translation.\n" +
                "##Respond with only the extracted text. -- just the extracted text.\n";
        var userMessage = new UserMessage(
                text,
                List.of(new Media(MimeTypeUtils.IMAGE_JPEG, resource)));
        return model.call(new Prompt(userMessage, ChatOptions.builder().model(OllamaModel.LLAVA.getName()).build()))
                .getResult().getOutput().getText();
    }

    @GetMapping("/streaming/picture/ocr")
    public Flux<String> streamingChat(@RequestParam("name") String name) {
        LRUCache<String, String> cache = cacheHandler.getCache("ocr_pic", 20, 10 * 60 * 1000);
        String v = cache.get(name);
        if (null != v) {
            System.out.println("cache hit");
            return Flux.just(v);
        }
        String imagePath = uploadPath + File.separator + name;
        FileSystemResource resource = new FileSystemResource(imagePath);
        var promptText = "##The user provides a picture and you need to extract the text in the picture. You will only respond with Chinese and English characters.\n"
                +
                "##The text in the picture is formatted from top to bottom, and within each line, from left to right.\n"
                +
                "##You must not classify them by language.\n" +
                "##You must extract each character in the original order.\n" +
                "##Ignore all line breaks in the extracted text and respond with a single continuous string.\n" +
                "##If the text in the picture cannot be recognized, respond with '无法识别'.\n" +
                "##Do not answer with any explanation, imagination or translation.\n" +
                "##Respond with only the extracted text. -- just the extracted text.\n";
        var userMessage = new UserMessage(
                promptText,
                List.of(new Media(MimeTypeUtils.IMAGE_JPEG, resource)));
        StringBuilder fullAnswerBuilder = new StringBuilder();
        Flux<ChatResponse> stream = model.stream(new Prompt(userMessage, ChatOptions.builder().model("minicpm-v:8b")
                .temperature(0.1)
                .maxTokens(4096)
                .build()));
        return stream.map(chatResp -> chatResp.getResult().getOutput().getText().trim())
                .doOnNext(chunk -> {
                    fullAnswerBuilder.append(chunk);
                })
                .doOnComplete(() -> {
                    String fullAnswer = fullAnswerBuilder.toString();
                    if (!fullAnswer.isEmpty()) {
                        cache.put(name, fullAnswer);
                    }
                });
    }

    @PostMapping(value = "/file/upload", consumes = "multipart/form-data", produces = "application/json")
    public Mono<ApiResponse<String>> uploadFile(@RequestPart("file") FilePart file)throws IllegalStateException, IOException {
        String filePath = uploadPath + File.separator + file.filename();
        File dest = new File(filePath);
        return file.transferTo(dest)
                .then(Mono.fromCallable(() -> {
                    return ApiResponse.success(filePath);
                }))
                .onErrorResume(e -> {
                    e.printStackTrace();
                    return Mono.just(ApiResponse.error());
                });
    }
}
