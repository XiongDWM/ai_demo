package com.xiongdwm.ai_demo.chat;

import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import com.xiongdwm.ai_demo.tools.DataBaseTool;
import com.xiongdwm.ai_demo.tools.EmbeddingTool;
import com.xiongdwm.ai_demo.tools.FiberTools;
import com.xiongdwm.ai_demo.tools.SearchRouteParam;
import com.xiongdwm.ai_demo.utils.JacksonUtil;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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

        private final WebClient webClient = WebClient.create("http://192.168.0.77:18081");

        // CoT  marked data example:
        // {
        // "question": "question",
        // "chain_of_thought": [
        // "1. intent： xxx",
        // "2. call tool1，param:'xxx'， description。",
        // "3. call tool2 param:"yyy" description。",
        // "4. xxxx "
        // ],
        // "tool_calls": [
        // {
        // "tool": "tool1",
        // "input": "xxx",
        // "output": "[aaa, bbb]"
        // },
        // {
        // "tool": "tool2",
        // "input": "xxx",
        // "output": "{...detail...}"
        // }
        // // ...more
        // ],
        // "score": 0.9,
        // "answer": "..."
        // }

        @PostMapping("/agent/chat")
        public Flux<String> chat(@RequestParam(name = "message") String message,
                        @RequestHeader(value = "chat-id", required = false) String chatId) {
                ToolCallback[] toolCallbacks = ToolCallbacks.from(fiberTool, embeddingTool, dataBaseTool);
                StringBuilder sb = new StringBuilder();
                sb.append("##你是一名智能助手 \n");
                sb.append("##请调用工具来回答用户的问题 \n");
                sb.append("##请分步思考，合理拆解用户的问题，并在每一步根据需要调用合适的工具。\n");
                sb.append("##每一步都要说明意图、工具、参数和预期结果 \n");
                sb.append("##请勿重复调用工具，等待工具返回结果后再继续");
                sb.append("##可以多步调用多个工具，直到完成任务目标。\n");
                sb.append("##如果无法调用工具，请直接用问题作为回答:"+message+" \n");
                sb.append("##请直接返回工具调用的结果，不要添加其他内容 \n");
                sb.append("##你需要按照规定格式返回结果，如下：\n");
                sb.append("##子问题:查询aaa和bbb之间的通路,参数:[起始点:aaa,终止点:bbb],结果:[xxx] \n");
                sb.append("##问题如下: \n").append(message).append("\n");
                ChatModel chatModel = OllamaChatModel.builder().ollamaApi(OllamaApi.builder().build()).build();
                String conversationId = chatId + "-" + System.currentTimeMillis();

                ChatOptions chatOption = ToolCallingChatOptions.builder()
                                .model("qwen3:4b")
                                .toolCallbacks(toolCallbacks)
                                .build();
                Prompt prompt = new Prompt(sb.toString(), chatOption);
                String response = ChatClient.create(chatModel)
                                .prompt(prompt)
                                .call()
                                .content();
                var toolCallsResult=ChatUtils.extractAnswerOnly(response);
                System.out.println(toolCallsResult);

                sb.setLength(0);
                sb.append("##你是一名智能助手");
                sb.append("##用户提供给一些数据,你需要根据这些数据来回答用户的问题\n");
                sb.append("##数据如下: \n").append(toolCallsResult).append("\n");
                sb.append("##用户问题如下：\n");
                sb.append(message).append("\n");
                sb.append("##请注意数字，不要篡改数字\n");
                Prompt promptOverAllPrompt = new Prompt(sb.toString());
                
                return ollamaChatModel.stream(promptOverAllPrompt)
                                .map(chatResp -> chatResp.getResult().getOutput().getText())
                                .doOnNext(chunk -> {
                                        System.out.print(chunk);
                                })
                                .map(chunk -> {
                                    ConversationContext ctx = new ConversationContext(chunk, conversationId);
                                    return JacksonUtil.toJsonString(ctx).get() + "</chunk>";
                                })
                                .doOnCancel(() -> {
                                        System.out.println("回答取消");
                                });
        }

        @PostMapping("/agent/chat/evaluate")
        public Flux<String> evaluate(@RequestParam(name="chatId")String chatId,
                        @RequestParam(name = "score") boolean score) {
                return Flux.just("已评价");
        }

        @PostMapping("/agent/chat/workflow")
        public Flux<String> chatOllama(@RequestParam(name = "message") String message) {
                ToolCallback[] toolCallbacks = ToolCallbacks.from(fiberTool, embeddingTool, dataBaseTool);
                ChatClient chatClient = ChatClient.create(ollamaChatModel);

                return Flux.just("");
        }

        @PostMapping("/agent/test/tool")
        public Mono<String> findPath(
            @RequestParam(name="fromStationName") String fromStationName,
            @RequestParam(name="toStationName") String toStationName,
            @RequestParam(name="maxHops") int maxHops,
            @RequestParam(name="count") int count,
            @RequestParam(name="maxDistance") double maxDistance) 
        {
        System.out.println("findpath");
        
        var weight = maxHops==0 ? 10.0d:maxHops*2.0d;

        SearchRouteParam searchParam = new SearchRouteParam(fromStationName,toStationName,weight,count,maxDistance);
        return webClient.post()
                .uri("/rel/searchRouteByStationName")
                .bodyValue(searchParam)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                .map(list -> {
                        if (list == null || list.isEmpty()) return "无可用路径";
                        StringBuilder sb = new StringBuilder();
                        int i = 1;
                        for (Map<String, Object> path : list) {
                        sb.append("路径").append(i++).append(": ")
                        .append(path.get("routes").toString()).append("; ").append("接入距离: ").append(path.get("buildDistance").toString())
                        .append("米");
                        sb.append("\n");
                        }
                        return sb.toString();
                });
    }
}
