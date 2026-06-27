package org.example.common.response;

import org.example.common.exception.ErrorCode;

public class ResultUtils {
    /**
     * 成功
     * @param <T>
     * @param data
     * @return
     */
    public static <T> BaseResponse<T> success(T data){
        return new BaseResponse(0,data,"ok");
    }
    /**
     * 失败
     * @param errorCode
     * @return
     */
    public static BaseResponse error(ErrorCode errorCode){
        return new BaseResponse<>(errorCode);
    }

    /**
     * 失败
     * @param code
     * @param message
     * @return
     */
    public static BaseResponse error(int code, String message){
        return new BaseResponse<>(code,null,message);
    }

    /**
     * 失败
     *
     * @param errorCode
     * @param message
     * @return
     */
    public static BaseResponse error(ErrorCode errorCode,String message){
        return new BaseResponse<>(errorCode.getCode(),null,message);
    }

}
