package com.xiongdwm.ai_demo.utils.global;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class StepGrouper {
    // 支持数字编号、字母编号（A. A) A、 A：）以及中文/英文 "步骤1"、"Step 1"
    // 例子："步骤1 ...", "Step 1 ...", "1. ...", "1) ...", "A. ...", "A) ...", "A、...", "A：..."
    private static final Pattern STEP_HEAD = Pattern.compile(
            "^\\s*(?:步骤\\s*\\d+|Step\\s*\\d+|\\d+[.)]|[A-Za-z][.)\\u3001\\uff1a])\\s*",
            Pattern.CASE_INSENSITIVE);

    /**
     * Group ordered DocBlock list into StepItem list.
     * Behavior:
     * - If the document contains any explicit step header (匹配 STEP_HEAD), use them as boundaries.
     * - If no explicit step headers are found, return a single StepItem that aggregates all text and images
     *   (so screenshots are attached to the section but steps are not artificially created).
     */
    public static List<StepItem> groupBlocksToSteps(List<DocBlock> blocks) {
        List<StepItem> steps = new ArrayList<>();
        if (blocks == null || blocks.isEmpty()) return steps;

        // Detect whether there is any explicit step header in the blocks
        boolean hasStepHeader = false;
        for (DocBlock b : blocks) {
            if (b.getType() == DocBlock.Type.TEXT) {
                String txt = b.getText() == null ? "" : b.getText().trim();
                if (!txt.isEmpty() && STEP_HEAD.matcher(txt).find()) {
                    hasStepHeader = true;
                    break;
                }
            }
        }

        // If no explicit step headers, aggregate everything into a single step
        if (!hasStepHeader) {
            StepItem single = new StepItem(1);
            for (DocBlock b : blocks) {
                if (b.getType() == DocBlock.Type.TEXT) {
                    String txt = b.getText() == null ? "" : b.getText().trim();
                    if (!txt.isEmpty()) single.appendText(txt);
                } else if (b.getType() == DocBlock.Type.IMAGE) {
                    single.addImage(b.getImageUrl());
                }
            }
            steps.add(single);
            return steps;
        }

        // Otherwise, use the explicit step header logic
        StepItem current = null;
        int idx = 0;

        for (DocBlock b : blocks) {
            if (b.getType() == DocBlock.Type.TEXT) {
                String txt = b.getText() == null ? "" : b.getText().trim();
                if (txt.isEmpty()) continue;

                if (STEP_HEAD.matcher(txt).find()) {
                    idx++;
                    current = new StepItem(idx);
                    // 去掉步骤头的前缀
                    String cleaned = txt.replaceFirst(STEP_HEAD.pattern(), "").trim();
                    if (cleaned.isEmpty()) cleaned = txt;
                    current.appendText(cleaned);
                    steps.add(current);
                } else {
                    if (current == null) {
                        idx++;
                        current = new StepItem(idx);
                        steps.add(current);
                    }
                    current.appendText(txt);
                }
            } else if (b.getType() == DocBlock.Type.IMAGE) {
                if (current == null) {
                    idx++;
                    current = new StepItem(idx);
                    steps.add(current);
                }
                current.addImage(b.getImageUrl());
            }
        }

        return steps;
    }
}
