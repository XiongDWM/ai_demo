package com.xiongdwm.ai_demo.chat;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.xiongdwm.ai_demo.tools.DataBaseTool;
import com.xiongdwm.ai_demo.tools.EmbeddingTool;
import com.xiongdwm.ai_demo.tools.FiberTools;

import reactor.core.publisher.Flux;

@RestController
public class AgentApi {

        @Autowired
        private FiberTools fiberTool;
        @Autowired
        private EmbeddingTool embeddingTool;
        @Autowired
        private DataBaseTool dataBaseTool;

        @Autowired
        private OllamaChatModel ollamaChatModel;

        @PostMapping("/agent/chat")
        public Flux<String> chat(@RequestParam(name = "message") String message,
                        @RequestHeader(value = "chat-id", required = false) String chatId) {
                ToolCallback[] toolCallbacks = ToolCallbacks.from(fiberTool, embeddingTool, dataBaseTool);
                StringBuilder sb = new StringBuilder();
                sb.append("##你是一名智能助手\n");
                sb.append("##请调用工具来回答用户的问题\n");
                sb.append("##可以多步调用多个工具\n");
                sb.append("##如果无法调用工具，请直接回答:无工具调用\n");
                sb.append("##请直接返回工具调用的结果，不要添加其他内容\n");
                sb.append("##问题如下: \n").append(message).append("\n");
                ChatModel chatModel = OllamaChatModel.builder().ollamaApi(OllamaApi.builder().build()).build();

                ChatOptions chatOption = ToolCallingChatOptions.builder()
                                .model("qwen3:4b")
                                .toolCallbacks(toolCallbacks)
                                .build();
                Prompt prompt = new Prompt(sb.toString(), chatOption);
                String response = ChatClient.create(chatModel)
                                .prompt(prompt)
                                .call()
                                .content();

                sb.setLength(0);
                sb.append("##你是一名智能助手");
                sb.append("##用户提供给一些数据,你需要根据这些数据来回答用户的问题\n");
                sb.append("##数据如下: \n").append(response).append("\n");
                sb.append("##用户问题如下：\n");
                sb.append(message).append("\n");
                sb.append("##请注意数字，不要篡改数字\n");
                Prompt promptOverAllPrompt = new Prompt(sb.toString());
                return ollamaChatModel.stream(promptOverAllPrompt)
                                .map(chatResp -> chatResp.getResult().getOutput().getText())
                                .doOnNext(chunk -> {
                                        System.out.print(chunk);
                                }).doOnCancel(() -> {
                                        System.out.println("回答取消");
                                });
        }

        @PostMapping("/agent/chat/workflow")
        public Flux<String> chatOllama(@RequestParam(name = "message") String message) {
                ToolCallback[] toolCallbacks = ToolCallbacks.from(fiberTool, embeddingTool, dataBaseTool);
                ChatClient chatClient = ChatClient.create(ollamaChatModel);
        
                

                return Flux.just("");       
        }     
}
