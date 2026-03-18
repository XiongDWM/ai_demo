package com.xiongdwm.ai_demo.webapp.resource;

import java.util.List;

import com.xiongdwm.ai_demo.utils.global.HierarchicalWordSplitHelper;
import com.xiongdwm.ai_demo.utils.global.SectionNode;
import com.xiongdwm.ai_demo.embedding.ingest.ImageCaptionClient;
import com.xiongdwm.ai_demo.utils.global.ApiResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DocParseController {

    @Autowired
    private HierarchicalWordSplitHelper hierarchicalWordSplitHelper;

    @Autowired
    private ImageCaptionClient imageCaptionClient;

    /**
     * Parse a local docx file and return hierarchical sections with steps and image URLs.
     * Example: POST /doc/parse?path=D:\\folder\\file.docx
     */
    @PostMapping("/doc/parse")
    public ApiResponse<Object> parseDoc(@RequestParam("path") String path,
                                                    @RequestParam(value = "imageSaveDir", required = false, defaultValue = "upload/images") String imageSaveDir) {
        try {
            List<SectionNode> sections = hierarchicalWordSplitHelper.parseHierarchy(path, imageSaveDir, imageCaptionClient, 1200);
            return ApiResponse.success(sections);
        } catch (Exception e) {
            return ApiResponse.error("解析失败: " + e.getMessage());
        }
    }
}
