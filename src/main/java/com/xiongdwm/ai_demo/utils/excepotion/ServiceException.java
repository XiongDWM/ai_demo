package com.xiongdwm.ai_demo.utils.excepotion;

public class ServiceException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private String message;

    public ServiceException(String message) {
        super(message);
        this.message = message;
    }
    
    public String getMessage() {
        return message;
    }
    
}
