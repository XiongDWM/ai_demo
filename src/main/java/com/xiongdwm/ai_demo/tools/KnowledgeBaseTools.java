package com.xiongdwm.ai_demo.tools;


import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class KnowledgeBaseTools {
//    public StringBuilder

    @Tool(description = "根据输入的文档内容，进行上下文扩展，返回上下文的文档列表")
    public List<Document> readDocWithScale(@ToolParam(description = "当前文档") Document document,
                                           @ToolParam(description = "向上或者向下查询，只能传入1或者-1，向下传入1，向上传入-1") int upOrDown,
                                           @ToolParam(description = "扩展阅读的大小，传入整数") int scale) {
        String content = ((String) document.getMetadata().get("fileUnique"));
        var currentIndex=((Integer) document.getMetadata().get("index"));
        


        return  new ArrayList<>();
    }
}
