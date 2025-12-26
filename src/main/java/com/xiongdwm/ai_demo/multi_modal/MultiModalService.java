package com.xiongdwm.ai_demo.multi_modal;

import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;

import com.xiongdwm.ai_demo.utils.GeometryUtils;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;

import java.util.List;

@Component
public class MultiModalService {
    @Autowired
    private OllamaChatModel model;

    public double calAngle(String coordsString){
        var parts = coordsString.split(";");
        var topCoords = parts[0].replace("top:", "").split(",");                
        var bottomCoords = parts[1].replace("bottom:", "").split(",");
        double[] top = {Double.parseDouble(topCoords[0]), Double.parseDouble(topCoords[1])};
        double[] bottom = {Double.parseDouble(bottomCoords[0]), Double.parseDouble(bottomCoords[1])};
        var angle = GeometryUtils.calAngle(top[0], top[1], bottom[0], bottom[1]);
        return angle;
    }

    public String captionImage(String imageUrl){
        // Dummy implementation for image captioning
        FileSystemResource resource = new FileSystemResource(imageUrl);
        var promptText = "##用户提供图片，你需要描述图片内容"
                + "##请描述图片中的内容，尽量详细，尽量简洁。\n"
                + "##注意图片中的物体、场景、动作等。\n"
                + "##请注意不要添加任何解释或想象的内容。\n"
                + "##只需返回图片内容描述，不要添加其他信息。\n";
        var userMessage = new UserMessage.Builder()
                .text(promptText)
                .media(List.of(new Media(MimeTypeUtils.IMAGE_JPEG, resource)))
                .build();
        StringBuilder fullAnswerBuilder = new StringBuilder();
        Flux<ChatResponse> stream = model.stream(new Prompt(userMessage, ChatOptions.builder()
                .model("qwen2.5vl:7b")
                .temperature(0.1)
                .maxTokens(4096)
                .build()));
        stream.map(chatResp -> {
                    String text = chatResp.getResult().getOutput().getText();
                    return text != null ? text.trim() : "";
                })
                .doOnNext(fullAnswerBuilder::append).blockLast();

        return fullAnswerBuilder.toString();
    }
}
