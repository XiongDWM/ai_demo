package com.xiongdwm.ai_demo.utils.opencv;

import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.global.opencv_imgproc;


public class InclineDetector {

    /**
     * @param imagePath       图片路径
     * @param roiX            ROI左上角x
     * @param roiY            ROI左上角y
     * @param roiW            ROI宽
     * @param roiH            ROI高
     * @param lengthThreshold 长度阈值
     * @param aspectThreshold 长宽比阈值
     * @return true=倾斜，false=未倾斜
     *  ** roi region of interest
     */
    public static boolean isObjectInclined(String imagePath, int roiX, int roiY, int roiW, int roiH,
            double lengthThreshold, double aspectThreshold) {
        Mat src = opencv_imgcodecs.imread(imagePath);
        if (src.empty())
            return false;

        Rect roi = new Rect(roiX, roiY, roiW, roiH);
        Mat roiMat = new Mat(src, roi);

        // 灰度
        Mat gray = new Mat();
        opencv_imgproc.cvtColor(roiMat, gray, opencv_imgproc.COLOR_BGR2GRAY);

        // 高斯模糊
        opencv_imgproc.GaussianBlur(gray, gray, new Size(5, 5), 0);

        Mat edges = new Mat();
        opencv_imgproc.Canny(gray, edges, 50, 150);

        MatVector contours = new MatVector();
        Mat hierarchy = new Mat();
        opencv_imgproc.findContours(edges, contours, hierarchy, opencv_imgproc.RETR_EXTERNAL,
                opencv_imgproc.CHAIN_APPROX_SIMPLE);

        double maxLen = 0;
        double maxAspect = 0;
        for (long i = 0; i < contours.size(); i++) {
            Mat contour = contours.get(i);
            RotatedRect rect = opencv_imgproc.minAreaRect(new Mat(contour));
            double len = Math.max(rect.size().height(), rect.size().width());
            double wid = Math.min(rect.size().height(), rect.size().width());
            double aspect = len / (wid + 1e-5);
            if (len > maxLen)
                maxLen = len;
            if (aspect > maxAspect)
                maxAspect = aspect;
        }

        // 满足长度和长宽比阈值即判定为倾斜
        return maxLen > lengthThreshold && maxAspect > aspectThreshold;
    }
}
