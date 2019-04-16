package com.dlong.rep.dleventbus.handler;

import com.dlong.rep.dleventbus.model.DLSubscription;

/**
 * 事件接口
 * @author  dlong
 * created at 2019/4/16 10:34 AM
 */
public interface IEventHandler {
    void handleEvent(DLSubscription subscription, Object message);
}
