package com.tianwj.exception;

/**
 * 图形转换异常
 *
 * @author tianwj
 */
public class PolygonConvertException extends RuntimeException {

    public PolygonConvertException() {
        super("convert error");
    }

    public PolygonConvertException(String message) {
        super(message);
    }

    public PolygonConvertException(String message, Throwable cause) {
        super(message, cause);
    }

    public PolygonConvertException(Throwable cause) {
        super("convert error", cause);
    }

}
