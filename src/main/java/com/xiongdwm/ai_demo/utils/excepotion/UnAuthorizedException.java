package com.xiongdwm.ai_demo.utils.excepotion;

public class UnAuthorizedException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private String message;
    
    public UnAuthorizedException(String message) {
        super(message);
        this.message = message;
    }
    
    public String getMessage() {
        return message;
    }
}
