package org.example.gateway.exception;

/**
 * Gateway 错误码枚举
 */
public enum GatewayErrorCode {

    NOT_LOGIN(40100, "未登录"),
    NO_AUTH(40101, "无权限"),
    BANNED(40300, "账号已被封禁"),
    SERVICE_DEGRADE(50300, "服务降级"),
    SYSTEM_ERROR(50000, "系统内部异常");

    private final int code;
    private final String message;

    GatewayErrorCode(int code, String message) {
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
