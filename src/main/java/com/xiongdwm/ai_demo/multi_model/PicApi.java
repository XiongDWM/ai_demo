package com.xiongdwm.ai_demo.multi_model;

import java.io.File;
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
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.ai.chat.model.ChatResponse;

import reactor.core.publisher.Flux;

@RestController
public class PicApi {
    @Autowired
    private OllamaChatModel model;

    @Value("${file.upload.path}")
    private String uploadPath;

    @GetMapping(value = "/mm/picture/ocr")
    public String extractTextFromImage(@RequestParam("name") String name) {

        String imagePath = uploadPath + File.separator + name;
        FileSystemResource resource = new FileSystemResource(imagePath);
        var text1="请用中文描述这张图片的内容";
        var text2="请描提取这张图片中的文字，注意你的回答需要包含中文、数字和英文不需要考虑语义，必须提取所有文字，并且只返回提取到的文字，不要做任何解释以及翻译：";
        var userMessage=new UserMessage(
            text2,
        List.of(new Media(MimeTypeUtils.IMAGE_JPEG,resource)));
        return model.call(new Prompt(userMessage,ChatOptions.builder().model(OllamaModel.LLAVA.getName()).build())).getResult().getOutput().getText();
    }

    @GetMapping("/streaming/picture/ocr")
    public Flux<String> streamingChat(@RequestParam("name") String name) {
        String imagePath = uploadPath + File.separator + name;
        FileSystemResource resource = new FileSystemResource(imagePath);
        var text="##The user provides a picture and you need to extract the text in the picture. You will only respond with Chinese and English characters.\n"+
        "##The text in the picture formatted from left to right and in multiple lines.\n"+
        "##You must not to classify them by language.\n"+
        "##You must extract each character in the origin order.\n"+
        "##Respond with only the extracted text. Do not answer with any explanation, imagination or translation -- just the extracted text.\n";
        var textCN="请提取这张图片中的文字，注意你的回答需要包含中文、数字和英文不需要考虑语义，必须提取所有文字，并且只返回提取到的文字，不要做任何解释以及翻译：";
        var textCN2="请用中文描述这张图片的内容";
        var userMessage=new UserMessage(
            textCN2,
        List.of(new Media(MimeTypeUtils.IMAGE_JPEG,resource)));
        Flux<ChatResponse> stream = model.stream(new Prompt(userMessage));  // ChatOptions.builder().model(OllamaModel.LLAVA.getName()).build()
        return stream.map(chatResp -> chatResp.getResult().getOutput().getText());
    }   
}
