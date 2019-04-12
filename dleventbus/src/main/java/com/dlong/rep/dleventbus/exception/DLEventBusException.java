package com.dlong.rep.dleventbus.exception;

/**
 * 自定义异常消息
 * @author  dlong
 * created at 2019/4/12 10:28 AM
 */
public class DLEventBusException extends RuntimeException {

    public DLEventBusException(String detailMessage) {
        super(detailMessage);
    }

    public DLEventBusException(Throwable throwable) {
        super(throwable);
    }

    public DLEventBusException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }
}
