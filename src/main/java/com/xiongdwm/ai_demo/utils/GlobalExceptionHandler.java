package com.xiongdwm.ai_demo.utils;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.xiongdwm.ai_demo.utils.excepotion.ServiceException;
import com.xiongdwm.ai_demo.utils.excepotion.UnAuthorizedException;
import com.xiongdwm.ai_demo.utils.global.ApiResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

//    @ExceptionHandler(Exception.class)
//    public ApiResponse<String> handleException(Exception e) {
//        logger.error("Global Exception Handler: ", e.getLocalizedMessage());
//        return ApiResponse.error(e.getLocalizedMessage());
//    }

    @ExceptionHandler(ServiceException.class)
    public ApiResponse<String> handleServiceException(ServiceException e) {
        logger.error("Service Exception: ", e.getLocalizedMessage());
        return ApiResponse.bussiness_error(e.getLocalizedMessage());
    }

    @ExceptionHandler(UnAuthorizedException.class)
    public ApiResponse<String> handleUnAuthorizedException(UnAuthorizedException e) {
        logger.error("Unauthorized Exception: ", e.getLocalizedMessage());
        return ApiResponse.unauthorized();
    }
    
}
