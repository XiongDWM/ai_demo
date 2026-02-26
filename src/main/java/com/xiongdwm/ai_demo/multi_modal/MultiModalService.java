package com.xiongdwm.ai_demo.multi_modal;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import io.micrometer.common.util.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Component;

import com.xiongdwm.ai_demo.utils.GeometryUtils;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.RequestPart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;

@Component
public class MultiModalService {
    @Autowired
    @Qualifier("ollamaChat")
    private ChatModel model;
    @Qualifier("dashscopeChat")
    @Autowired
    private ChatModel dashscope;

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
                .model("qwen2.5vl:3b")
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

    public List<String> extractTextInline(String imageUrl){
        // Dummy implementation for image captioning
        List<String>result=new ArrayList<>();
        FileSystemResource resource = new FileSystemResource(imageUrl);
        var promptText = "用户提供图片，请按图片中行的顺序逐行提取其中的文字。"
                + "要求：\n"
                + "1\\) 只输出图片内的原文文本，每一行对应图片中的一行文字；\n"
                + "2\\) 不要添加任何额外说明、编号或格式化；\n"
                + "3\\) 保持文本原样，不要改写或补全缺失内容；\n"
                + "4\\) 输出的每一行用换行符分隔（UNIX 风格），确保顺序与图片一致。";
        var userMessage = new UserMessage.Builder()
                .text(promptText)
                .media(List.of(new Media(MimeTypeUtils.IMAGE_JPEG, resource)))
                .build();
        StringBuilder buffer = new StringBuilder();
        Flux<ChatResponse> stream = model.stream(new Prompt(userMessage, ChatOptions.builder()
                .model("qwen2.5vl:3b")
                .temperature(0.7)
                .maxTokens(4096)
                .build()));
        stream.map(chatResp -> {
                    String text = chatResp.getResult().getOutput().getText();
                    return text != null ? text.trim() : "";
                })
                .doOnNext(context->{
                    if(StringUtils.isBlank(context))return;
                    buffer.append(context);
                    int index=0;
                    while((index=buffer.indexOf("\n"))!=-1){
                        String line=buffer.substring(0,index).replaceAll("\\r","").trim();
                        if(!line.isEmpty()){
                            result.add(line);
                        }
                        buffer.delete(0,index+1);
                    }
                }).blockLast();
        String last = buffer.toString().trim();
        if (!last.isEmpty()) {
            result.add(last);
        }
        return result;
    }

    public Mono<String> multimodalDeviceScaleReader(String fileName){
        var subfix=fileName.substring(fileName.lastIndexOf(".")+1);
        if(subfix.equals("doc")||subfix.equals("docx")){
            // 调用解析
            return Mono.just("还在做!!");
        }
        System.out.println(subfix);
        String promptBuilder = """
                你是一个多模态解析助手，用户提供图片，ppt或者pdf文件，请你根据文件内容提取设备规格以及设备参数的信息。
                请按照以下格式提取信息：
                    @@@设备名称：XXX
                    @@@设备规格：XXX
                    @@@设备参数：XXX
                请勿篡改规格参数信息
                请确保提取的信息准确且完整，如果文件中没有相关信息，请返回'无相关信息'。""";
            var options= DashScopeChatOptions.builder()
                .enableThinking(true)
                .model("qwen3.5-plus")
                .multiModel(true)
                .build();
            var resource=new FileSystemResource(fileName);
            var userMessageBuilder = new UserMessage.Builder()
                    .text(promptBuilder);
            switch (subfix){
                case "jpg","jpeg","png"-> userMessageBuilder.media(List.of(new Media(MimeTypeUtils.IMAGE_JPEG, resource)));
                default -> userMessageBuilder.media(List.of(new Media(MimeTypeUtils.ALL,resource)));
            }
            var userMessage=userMessageBuilder.build();

        ChatClient client=ChatClient.builder(dashscope).build();
        return client.prompt(new Prompt(userMessage, options))
                .stream()
                .chatResponse()
                .map(cr -> {
                    String text = cr.getResult().getOutput().getText();
                    return text == null ? "" : text.trim();
                })
                .filter(StringUtils::isNotBlank)
                .reduce(new StringBuilder(), StringBuilder::append)
                .map(sb -> sb.toString().trim().replace("@@@", "\n--"));

    }
}
