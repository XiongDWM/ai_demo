package com.xiongdwm.ai_demo.utils.global;

public class ApiResponse<T>{
    
    private T data;
    private int code;
    private boolean success;

    public ApiResponse(T data, int code, boolean success) {
        this.data = data;
        this.code = code;
        this.success = success;
    }

    public enum ApiResponseCode {
        INTERAL_SERVER_ERROR(500,"Internal Server Error"),
        SUCCESS(200,"Success"),
        BUSSINESS_ERROR(555,"Business Error"),
        UNAUTHORIZED(401,"Unauthorized");

        private final int code;
        private String message;

        ApiResponseCode(int code, String message) {
            this.code = code;
            this.message = message;
        }

        public int getCode() {
            return code;
        }
        public String getMessage() {
            return message;
        }
    }

    public static ApiResponse<String> success() {
        return new ApiResponse<>(ApiResponseCode.SUCCESS.getMessage(), ApiResponseCode.SUCCESS.getCode(), true);
    }

    public static<T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(data, ApiResponseCode.SUCCESS.getCode(), true);
    }

    public static<T> ApiResponse<T> error(T data) {
        return new ApiResponse<>(data, ApiResponseCode.INTERAL_SERVER_ERROR.getCode(), false);
    }

    public static ApiResponse<String> error() {
        return new ApiResponse<>(ApiResponseCode.INTERAL_SERVER_ERROR.getMessage(), ApiResponseCode.INTERAL_SERVER_ERROR.getCode(), false);
    }

    public static<T> ApiResponse<T> bussiness_error(T data){
        return new ApiResponse<>(data, ApiResponseCode.BUSSINESS_ERROR.getCode(), false);
    } 
    
    public static ApiResponse<String> unauthorized(){
        return new ApiResponse<>(ApiResponseCode.UNAUTHORIZED.getMessage(), ApiResponseCode.UNAUTHORIZED.getCode(), false);
    }

    public T getData() {
        return data;
    }

    public int getCode() {
        return code;
    }

    public boolean isSuccess() {
        return success;
    }
}
