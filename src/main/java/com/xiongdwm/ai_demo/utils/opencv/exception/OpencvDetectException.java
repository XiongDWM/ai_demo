package com.xiongdwm.ai_demo.utils.opencv.exception;

public class OpencvDetectException extends RuntimeException {
    public OpencvDetectException(String message) {
        super("Detect Error: "+message);
    }
}
