package org.example.gateway.exception;

/**
 * Gateway 业务异常
 */
public class GatewayException extends RuntimeException {

    private final int code;

    public GatewayException(GatewayErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public GatewayException(GatewayErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    public int getCode() {
        return code;
    }
}
