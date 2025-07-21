package com.xiongdwm.ai_demo.multi_modal;

import java.io.File;
import java.io.IOException;
import java.util.List;


import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.SecurityProperties.User;
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

import org.springframework.ai.chat.messages.UserMessage;
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
        var userMessage = UserMessage.builder()
                .text(text)
                .media(List.of(new Media(MimeTypeUtils.IMAGE_JPEG, resource)))
                .build();
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
        FileSystemResource resource = new FileSystemResource(name);
        var promptText = """
            ##你是OCR助手，只能逐字提取图片中的中文和英文字符，绝不能翻译、解释或补充。
            ##You are an OCR assistant. Only extract Chinese and English characters from the image, do NOT translate, interpret, or add anything.
            ##图片中的文字是什么就输出什么，保持原顺序，不要分类，不要解释，不要翻译，不要省略，不要想象。
            ##What you see is what you output. Keep the original order. No classification, no explanation, no translation, no imagination.
            ##如果无法识别，回复“无法识别”。
            ##If the text cannot be recognized, reply with '无法识别'.
            ##只输出提取到的文字，不要输出任何其他内容。
            ##Only output the extracted text, nothing else.
            ##输出时请忽略所有换行，所有文字合并为一个连续字符串。
            ##Ignore all line breaks in the output, merge all text into a single continuous string.
        """;
        var userMessage = new UserMessage.Builder()
                .text(promptText)
                .media(List.of(new Media(MimeTypeUtils.IMAGE_JPEG, resource)))
                .build();
        StringBuilder fullAnswerBuilder = new StringBuilder();
        Flux<ChatResponse> stream = model.stream(new Prompt(userMessage, ChatOptions.builder().model("minicpm-v:8b")
                .temperature(0.1)
                .maxTokens(4096)
                .build()));
        return stream.map(chatResp -> chatResp.getResult().getOutput().getText())
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

    @GetMapping(value = "/picture/ocr/content")
    public Flux<String> getOcrContent(@RequestParam("name") String name) {
        LRUCache<String, String> cache = cacheHandler.getCache("ocr_pic_content", 20, 10 * 60 * 1000);
        String v = cache.get(name);
        if (null != v) {
            System.out.println("cache hit");
            return Flux.just(v);
        }
        FileSystemResource resource = new FileSystemResource(name);
        var promptText = "##用户提供图片，你需要描述图片内容"
                + "##请描述图片中的内容，尽量详细。\n"
                + "##注意图片中的物体、场景、动作等。\n"
                + "##请注意不要添加任何解释或想象的内容。\n"
                + "##只需返回图片内容描述，不要添加其他信息。\n";
        var userMessage = new UserMessage.Builder()
                .text(promptText)
                .media(List.of(new Media(MimeTypeUtils.IMAGE_JPEG, resource)))
                .build();
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
