package com.xiongdwm.ai_demo.utils;

public class GeometryUtils {
    
    public static double calAngle(double x1, double y1, double x2, double y2) {
        double angle = Math.atan2(y2 - y1, x2 - x1);
        return Math.toDegrees(angle);
    }
    public static double calDistance(double x1, double y1, double x2, double y2) {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }
    public static double manhhatanDistance(double x1, double y1, double x2, double y2) {
        return Math.abs(x2 - x1) + Math.abs(y2 - y1);
    }
}
