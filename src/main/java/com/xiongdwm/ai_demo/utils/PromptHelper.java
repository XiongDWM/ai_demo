package com.xiongdwm.ai_demo.utils;

import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;

public class PromptHelper {

    public static Prompt buildToolCallingPromptZh(String userMessage, ChatOptions chatOption) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个具有有限工具集的自治智能体。请严格遵守下面规则并且只输出一个符合模式的 JSON 对象。\n");
        sb.append("规则：\n");
        sb.append("1. 只输出一个 JSON 对象并且仅输出该对象（不能有 Markdown、解释或额外文本）。\n");
        sb.append("2. 不要暴露思考链（chain-of-thought）。提供简明的 `plan` 数组，仅包含步骤说明，不包含内部推理。\n");
        sb.append("3. 允许将用户请求拆分为多个子任务并按顺序调用工具。每次调用工具后，必须把该调用的结果记录到 `tool_calls` 并在获得工具返回后再继续下一步。\n");
        sb.append("4. 若工具返回 JSON，请在 `tool_calls` 中同时保留原始 JSON 字符串字段 `raw_output`（不得改动或摘取字段）以及可选的已解析对象 `output`。示例：\n");
        sb.append("   {\"tool\":\"findPath\",\"input\":{...},\"output\":{...},\"raw_output\":\"<原始json字符串>\"}\n");
        sb.append("5. 保持工具返回的数值精确，不要修改或估算。\n");
        sb.append("6. 如果无法调用工具，请将 `tool_calls` 设为空数组并把 `final_answer` 设为 \"无法调用工具\"。\n");
        sb.append("\n");
        sb.append("返回模式（必须字段）：\n");
        sb.append("{\n");
        sb.append("  \"final_answer\": string,\n");
        sb.append("  \"confidence\": number,\n");
        sb.append("  \"plan\": [\n");
        sb.append("    {\"step\": number, \"intent\": string, \"tool\": string|null, \"input\": object|null, \"expected_output\": string}\n");
        sb.append("  ],\n");
        sb.append("  \"tool_calls\": [\n");
        sb.append("    {\"tool\": string, \"input\": object, \"output\": object|null, \"raw_output\": string|null}\n");
        sb.append("  ]\n");
        sb.append("}\n");
        sb.append("\n");
        sb.append("示例响应（保留原始 JSON）：\n");
        sb.append("{\n");
        sb.append("  \"final_answer\": \"找到两条可行路径，推荐路径 A（距离 120m）\",\n");
        sb.append("  \"confidence\": 0.85,\n");
        sb.append("  \"plan\": [\n");
        sb.append("    {\"step\":1, \"intent\":\"查找邻近接入点\", \"tool\":\"findPath\", \"input\":{\"from\":\"A\",\"to\":\"B\",\"maxHops\":2}, \"expected_output\":\"返回路径列表\"},\n");
        sb.append("    {\"step\":2, \"intent\":\"选择最短接入距离的路径\", \"tool\":null, \"input\":null, \"expected_output\":\"返回最短路径并给出推荐理由\"}\n");
        sb.append("  ],\n");
        sb.append("  \"tool_calls\": [\n");
        sb.append("    {\"tool\":\"findPath\", \"input\": {\"from\":\"A\",\"to\":\"B\",\"maxHops\":2}, \"output\": [{\"routes\":\"A->X->B\",\"buildDistance\":120}], \"raw_output\": \"[{\\\"routes\\\":\\\"A->X->B\\\",\\\"buildDistance\\\":120}]\"}\n");
        sb.append("  ]\n");
        sb.append("}\n");
        sb.append("\n");
        sb.append("用户问题：\n").append(userMessage).append("\n");
        sb.append("\n");
        sb.append("重要提示：仅使用提供的工具，不要伪造工具输出。`raw_output` 必须是工具原始返回的 JSON 字符串且不得被修改。输出应紧凑且可机器解析。\n");

        return new Prompt(sb.toString(), chatOption);
    }
}
