package com.xiongdwm.ai_demo.chat;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.xiongdwm.ai_demo.ingest.ExcelEvaluateService;
import com.xiongdwm.ai_demo.tools.DataBaseTool;
import com.xiongdwm.ai_demo.tools.EmbeddingTool;
import com.xiongdwm.ai_demo.tools.ExcelEvaluateTool;
import com.xiongdwm.ai_demo.tools.FiberTools;
import com.xiongdwm.ai_demo.utils.JacksonUtil;
import com.xiongdwm.ai_demo.utils.SearchRouteParam;
import com.xiongdwm.ai_demo.utils.global.ApiResponse;
import com.xiongdwm.ai_demo.webapp.entities.EvaluateExcelLog;
import com.xiongdwm.ai_demo.webapp.entities.EvaluateSnapshot;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;


@RestController
public class AgentApi {

        @Autowired
        private FiberTools fiberTool;
        @Autowired
        private EmbeddingTool embeddingTool;
        @Autowired
        private DataBaseTool dataBaseTool;
        @Autowired
        private ExcelEvaluateTool excelEvaluateTool;
        @Autowired
        private ExcelEvaluateService excelEvaluateService;

        @Autowired
        private OllamaChatModel ollamaChatModel;
        @Autowired
        @Qualifier("dashscopeChat")
        private ChatModel dashscopeChatModel;

        @Value("${file.upload.path}")
        private String uploadPath;
        

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
                sb.append("##你是一个智能体 \n");
                sb.append("##系统会提供工具，必要时需要调用工具获取结果来回答用户的问题 \n");
                sb.append("##请分步思考，合理拆解用户的问题，并在每一步根据需要调用合适的工具。\n");
                sb.append("##每一步都要说明意图、工具、参数和预期结果 \n");
                sb.append("##请勿重复调用工具，等待工具返回结果后再继续");
                sb.append("##可以多步调用多个工具，直到完成任务目标。\n");
                sb.append("##如果无法调用工具，返回：“无法调用工具” \n");
                sb.append("##请直接返回工具调用的结果，不要添加其他内容 \n");
                sb.append("##你需要按照规定格式返回结果，如下：\n");
                sb.append("##问题如下: \n").append(message).append("\n");
                ChatModel chatModel = OllamaChatModel.builder().ollamaApi(OllamaApi.builder().build()).build();
                String conversationId = chatId + "-" + System.currentTimeMillis();

                ChatOptions chatOption = ToolCallingChatOptions.builder()
                                // .model("qwen3:4b")
                                .model("nemotron-mini:4b")
                                .toolCallbacks(toolCallbacks)
                                .build();
                Prompt prompt = new Prompt(sb.toString(), chatOption);
                String response = ChatClient.create(chatModel)
                                .prompt(prompt)
                                .call()
                                .content();
                var toolCallsResult=ChatUtils.extractAnswerOnly(response);
                System.out.println("工具结果："+toolCallsResult);

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
                                .map(chunk -> {
                                    ConversationContext ctx = new ConversationContext(chunk, conversationId);
                                    return JacksonUtil.toJsonString(ctx).orElse(ConversationContext.getEmptyContextJsonString()) + "</chunk>";
                                })
                                .doOnNext(System.out::print)
                                .doOnCancel(() -> {
                                        System.out.println("回答取消");
                                });
        }
        @PostMapping("/agent/test/chat")
        public Mono<String> testChat(@RequestParam(name = "message") String message,
                        @RequestHeader(value = "chat-id", required = false) String chatId) {
                 ToolCallback[] toolCallbacks = ToolCallbacks.from(fiberTool);
                StringBuilder sb = new StringBuilder();
                sb.append("##你是一名智能助手 \n");
                sb.append("##请调用工具来回答用户的问题 \n");
                sb.append("##请分步思考，合理拆解用户的问题，并在每一步根据需要调用合适的工具。\n");
                sb.append("##每一步都要说明意图、工具、参数和预期结果 \n");
                sb.append("##可以多步调用多个工具，直到完成任务目标。\n");
                sb.append("##请务必等待工具返回结果，并且直接引用工具结果进行推理和回答\n");
                sb.append("##如果无法调用工具，请直接用问题作为回答:"+message+" \n");
                // sb.append("##注意禁止重复调用同一个工具：如果工具已经返回了明确的结果，请直接用该结果继续推理或回答，不要再次调用该工具。");
                sb.append("##问题如下: \n").append(message).append("\n");
                ChatModel chatModel = OllamaChatModel.builder().ollamaApi(OllamaApi.builder().build()).build();
                String conversationId = chatId + "-" + System.currentTimeMillis();
                ChatOptions chatOption = ToolCallingChatOptions.builder()
                                .model("qwen3:4b")
                                // .model("nemotron-mini:4b")
                                .toolCallbacks(toolCallbacks)
                                .build();
                Prompt prompt = new Prompt(sb.toString(), chatOption);
                var response = ChatClient.create(chatModel)
                                .prompt(prompt)
                                .call().content();
                var toolCallsResult=ChatUtils.extractAnswerOnly(response);
                System.out.println(toolCallsResult);
                System.out.println("工具结果："+response);
                return Mono.just(response);
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

    @PostMapping("/agent/rag/agent")
    private Flux<String> ragAgent(String message, String chatId, String attachFile, String knowledgeBaseChosen) {
        // TODO RAG Agent implementation
        return Flux.just("RAG Agent 未实现");
    }

    @PostMapping("/agent/test/toolV2")
    public Mono<String> testTool(@RequestParam(name="input") String input) {
        WebClient webClient = WebClient.create("http://192.168.0.66:18888");
        var response = webClient.post()
        //     .uri(uri->uri
        //         .path("/test/tool")
        //         .queryParam("input", input)
        //         .build()
        //     )
            .uri("/mcp/fiber/getIdByName")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(Map.of("name", input))
            .retrieve()
            .bodyToMono(Long.class);
        return response.map(result -> "测试工具成功，输入为：" + result);
    }

    // ==================== Excel 评估 Agent 接口 ====================

    /**
     * 步骤1a: 单独上传 Excel 文件 → 动态建表 + 插入数据 + 记录日志
     * 返回 EvaluateExcelLog（含 id、tableName、dbDescription）
     * 此时尚未关联 Snapshot，用户后续在 setup 时选择要关联的 logId。
     */
    @PostMapping(value = "/agent/excel/upload", consumes = "multipart/form-data", produces = "application/json")
    public Mono<ApiResponse<EvaluateExcelLog>> uploadExcel(@RequestPart("file") FilePart file) {
        return Mono.fromCallable(() -> {
            String evalDir = uploadPath + "/eval";
            new File(evalDir).mkdirs();
            String filePath = evalDir + "/" + file.filename();
            File dest = new File(filePath);
            file.transferTo(dest).block();

            EvaluateExcelLog log = excelEvaluateService.uploadExcel(filePath, file.filename());
            System.out.println("已上传 Excel: " + file.filename() + ", logId=" + log.getId() + ", table=" + log.getTableName());
            return ApiResponse.success(log);
        }).subscribeOn(Schedulers.boundedElastic())
        .onErrorResume(e -> {
            e.printStackTrace();
            return Mono.just(ApiResponse.error(null));
        });
    }

    /**
     * 查询所有未关联快照的 Excel 上传记录（待选列表）
     */
    @GetMapping("/agent/excel/unboundList")
    public Mono<ApiResponse<List<EvaluateExcelLog>>> listUnboundExcelLogs() {
        return Mono.fromCallable(() -> ApiResponse.success(excelEvaluateService.listUnboundExcelLogs()))
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(e -> Mono.just(ApiResponse.error(null)));
    }

    /**
     * 步骤1b: 创建评估快照 — 传入跨表规则 + 已上传的 excelLogId 列表
     * 后台会将这些 Log 关联到新快照，并注册表描述到专属向量库。
     *
     * @param tableRelation  跨表关系描述，如 "铁塔表的A列(站点编码)对应基站表的C列(站点编码)"
     * @param excelLogIds    已上传的 EvaluateExcelLog id 列表，逗号分隔
     * @return 创建好的 snapshotId
     */
    @PostMapping("/agent/excel/setup")
    public Mono<ApiResponse<Long>> setupExcelEvaluate(
            @RequestParam("tableRelation") String tableRelation,
            @RequestParam("excelLogIds") List<Long> excelLogIds) {

        return Mono.fromCallable(() -> {
            EvaluateSnapshot snapshot = excelEvaluateService.setupSnapshot(tableRelation, excelLogIds);
            System.out.println("创建评估快照: id=" + snapshot.getId() + ", tag=" + snapshot.getVectorTag()
                    + ", 关联 " + excelLogIds.size() + " 个 Excel");
            return ApiResponse.success(snapshot.getId());
        }).subscribeOn(Schedulers.boundedElastic())
        .onErrorResume(e -> {
            e.printStackTrace();
            return Mono.just(ApiResponse.error(null));
        });
    }

    /**
     * 步骤2: Excel 评估对话 — Agent 自动调用工具检索表结构、生成并执行 SQL、返回结果
     * 回答末尾会追问用户"是否需要导出结果为 Excel"
     *
     * @param snapshotId  评估快照 ID（从 setup 返回）
     * @param message     用户问题
     * @param chatId      会话 ID
     */
    @PostMapping("/agent/excel/chat")
    public Flux<String> excelEvaluateChat(
            @RequestParam("snapshotId") Long snapshotId,
            @RequestParam("message") String message,
            @RequestHeader(value = "chat-id", required = false) String chatId) {


        String conversationId = (chatId != null ? chatId : "eval") + "-" + System.currentTimeMillis();

        return Flux.create(sink -> {
            sink.next(JacksonUtil.toJsonString(
                new ConversationContext("【系统】正在分析 Excel 数据...", conversationId))
                .orElse(ConversationContext.getEmptyContextJsonString()) + "</chunk>");
            System.out.println("Agent 开始分析，调用工具检索表结构并执行 SQL, snapshotId=" + snapshotId);
            Mono.fromCallable(() -> {
                System.out.println("Agent 开始分析，调用工具检索表结构并执行 SQL");
                // 设置当前 snapshotId 供 Tool 使用
                ExcelEvaluateTool.setCurrentSnapshotId(snapshotId);
                try {
                    // Agent 调用工具：检索表描述 → 生成SQL → 执行SQL
                    ToolCallback[] toolCallbacks = ToolCallbacks.from(excelEvaluateTool);
                    var documents = excelEvaluateService.searchDescriptions(snapshotId, message);

                    StringBuilder agentPrompt = new StringBuilder();
                    agentPrompt.append("##你是一个数据库分析专家 Agent\n");
                    agentPrompt.append("##用户有数据库多张表的描述，你可以调用工具来回答问题\n");
                    agentPrompt.append("##数据库结构如下（请直接使用，不要再次检索）：\n");
                    for (Document doc : documents) {
                        agentPrompt.append(doc.getText()).append("\n");
                    }
                    agentPrompt.append("##工作流程：\n");
                    agentPrompt.append("  1. 请将用户的问题拆分为若干子问题，优先使用单表查询，尽量避免 JOIN。\n");
                    agentPrompt.append("  2. 对每个子问题生成单表 SQL 并调用 excelSqlExecute 执行。\n");
                    agentPrompt.append("  3. 每次工具调用后，应把工具返回的结果以结构化 JSON 的形式记录下来（见下述格式要求），并把该记录作为下一轮调用或最终汇总的依据。\n");
                    agentPrompt.append("  4. 汇总所有子问题的查询结果并返回最终回答。\n");
                    agentPrompt.append("##关于 SQL 的要求：\n");
                    agentPrompt.append(" - 列名用反引号包裹如 `A`，表名也用反引号。\n");
                    agentPrompt.append(" - 每次 SQL 只返回必要字段，避免 SELECT * 。\n");
                    agentPrompt.append("##关于工具调用的输出格式（强制）：\n");
                    agentPrompt.append(" - 请务必以 JSON 格式返回工具调用记录，顶层字段包含，格式如下：\n");
                    agentPrompt.append("   {\n");
                    agentPrompt.append("     \"tool_calls\": [\n");
                    agentPrompt.append("       { \"round\": \"this_month\", \"sql\": \"SELECT ...\", \"result\": [[\"接入\",5976],[\"用户\",22906],...] },\n");
                    agentPrompt.append("       { \"round\": \"last_month\", \"sql\": \"SELECT ...\", \"result\": [...] }\n");
                    agentPrompt.append("     ],\n");
                    agentPrompt.append("     \"final_summary\": \"简洁中文回答\"\n");
                    agentPrompt.append("   }\n");
                    agentPrompt.append(" - 每个 tool_calls 元素必须包含 'round'（轮次标签）、'sql'、'result'（数组，结构化数值）。\n");
                    agentPrompt.append(" - 在生成 final_summary 时，必须直接引用 tool_calls 中的数值并标明来源轮次，严禁重新计算或更改这些数值。\n");
                    agentPrompt.append("##用户问题：\n").append(message).append("\n");
                    System.out.println(agentPrompt.toString());

                    DashScopeChatOptions options=DashScopeChatOptions.builder()
                            .model("qwen3.5-flash")
                            .toolCallbacks(List.of(toolCallbacks))
                            .build();
                    var prompt=new Prompt(agentPrompt.toString(), options);
                    String toolResult = ChatClient.create(dashscopeChatModel)
                            .prompt(prompt)
                            .call().content();
                            System.out.println("Agent 原始工具调用结果: " + toolResult);

                    return toolResult;
                } finally {
                    ExcelEvaluateTool.clearCurrentSnapshotId();
                }
            }).subscribeOn(Schedulers.boundedElastic())
            .subscribe(toolResult -> {
                System.out.println("Excel Agent 工具结果: " + toolResult);

                sink.next(JacksonUtil.toJsonString(
                    new ConversationContext("【系统】数据查询完成，正在生成回答...", conversationId))
                    .orElse(ConversationContext.getEmptyContextJsonString()) + "</chunk>");

                // 程序化解析 toolResult（JSON），去重并生成最终汇总，避免再次调用工具或让 LLM 重新计算
                try {
                    var either = JacksonUtil.convertJsonStringToJsonNode(toolResult);
                    if (either == null) throw new RuntimeException("无法解析 toolResult 为 JSON");
                    JsonNode root = either.isArray() ? either.getNodeList().get(0) : either.getSingle();

                    // 解析 tool_calls 并按 sql 去重（以最后一次出现为准）
                    Map<String, JsonNode> sqlToCall = new LinkedHashMap<>();
                    if (root.has("tool_calls") && root.get("tool_calls").isArray()) {
                        for (JsonNode call : root.get("tool_calls")) {
                            String sql = call.has("sql") ? call.get("sql").asText() : UUID.randomUUID().toString();
                            sqlToCall.put(sql, call); // later calls override earlier ones
                        }
                    }

                    // 按 round 分组并构建汇总文本
                    Map<String, List<JsonNode>> roundGroups = new LinkedHashMap<>();
                    for (JsonNode call : sqlToCall.values()) {
                        String round = call.has("round") ? call.get("round").asText() : "default";
                        roundGroups.computeIfAbsent(round, k -> new ArrayList<>()).add(call);
                    }

                    StringBuilder finalSummary = new StringBuilder();
                    for (Map.Entry<String, List<JsonNode>> entry : roundGroups.entrySet()) {
                        finalSummary.append("=== ").append(entry.getKey()).append(" 统计结果：\n");
                        for (JsonNode call : entry.getValue()) {
                            JsonNode resultNode = call.get("result");
                            if (resultNode != null && resultNode.isArray()) {
                                for (JsonNode row : resultNode) {
                                    if (row.isArray() && row.size() >= 2) {
                                        finalSummary.append(row.get(0).asText()).append(" : ").append(row.get(1).asText()).append("\n");
                                    } else if (row.isObject() && row.has("key") && row.has("value")) {
                                        finalSummary.append(row.get("key").asText()).append(" : ").append(row.get("value").asText()).append("\n");
                                    }
                                }
                            }
                        }
                        finalSummary.append("\n");
                    }

                    // 先返回原始 toolResult（供前端审计），再返回程序化的 finalSummary
                    sink.next(JacksonUtil.toJsonString(new ConversationContext(toolResult, conversationId)).orElse(ConversationContext.getEmptyContextJsonString()) + "</chunk>");
                    sink.next(JacksonUtil.toJsonString(new ConversationContext(finalSummary.toString(), conversationId)).orElse(ConversationContext.getEmptyContextJsonString()) + "</chunk>");
                    sink.complete();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    sink.next(JacksonUtil.toJsonString(new ConversationContext("回答生成失败: " + ex.getMessage(), conversationId)).orElse(ConversationContext.getEmptyContextJsonString()) + "</chunk>");
                    sink.complete();
                }
            }, error -> {
                error.printStackTrace();
                sink.next(JacksonUtil.toJsonString(
                    new ConversationContext("【系统】分析失败: " + error.getMessage(), conversationId))
                    .orElse(ConversationContext.getEmptyContextJsonString()) + "</chunk>");
                sink.complete();
            });
        });
    }

    /**
     * 步骤3: 确认导出 — 将最近一次查询结果导出为 Excel
     */
    @PostMapping("/agent/excel/export")
    public Mono<ApiResponse<String>> excelExport(@RequestParam("snapshotId") Long snapshotId) {
        return Mono.fromCallable(() -> {
            EvaluateSnapshot snapshot = excelEvaluateService.getSnapshot(snapshotId);
            if (snapshot == null) return ApiResponse.error("未找到评估快照");
            if (snapshot.getLastSql() == null || snapshot.getLastSql().isEmpty()) {
                return ApiResponse.error("没有可导出的查询结果，请先提问");
            }
            String exportDir = uploadPath + "/export";
            String filePath = excelEvaluateService.exportToExcel(snapshot.getLastSql(), exportDir);
            return ApiResponse.success(filePath);
        }).subscribeOn(Schedulers.boundedElastic())
        .onErrorResume(e -> Mono.just(ApiResponse.error("导出失败: " + e.getMessage())));
    }

    /**
     * 清理评估快照关联的临时表
     */
    @PostMapping("/agent/excel/cleanup")
    public Mono<ApiResponse<String>> cleanupSnapshot(@RequestParam("snapshotId") Long snapshotId) {
        return Mono.fromCallable(() -> {
            excelEvaluateService.cleanupSnapshot(snapshotId);
            return ApiResponse.success("已清理临时表");
        }).subscribeOn(Schedulers.boundedElastic())
        .onErrorResume(e -> Mono.just(ApiResponse.error("清理失败: " + e.getMessage())));
    }

    /**
     * 查询所有评估快照列表（不分页）
     */
    @GetMapping("/agent/excel/snapshots")
    public Mono<ApiResponse<List<EvaluateSnapshot>>> listSnapshots() {
        return Mono.fromCallable(() -> ApiResponse.success(excelEvaluateService.listSnapshots()))
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(e -> Mono.just(ApiResponse.error(null)));
    }

    /**
     * 查询指定快照下的 Excel 上传记录列表（不分页）
     */
    @GetMapping("/agent/excel/logs")
    public Mono<ApiResponse<List<EvaluateExcelLog>>> listExcelLogs(@RequestParam("snapshotId") Long snapshotId) {
        return Mono.fromCallable(() -> ApiResponse.success(excelEvaluateService.listExcelLogs(snapshotId)))
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(e -> Mono.just(ApiResponse.error(null)));
    }
}
