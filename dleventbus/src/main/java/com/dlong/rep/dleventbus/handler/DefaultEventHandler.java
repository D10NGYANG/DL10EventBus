package com.dlong.rep.dleventbus.handler;

import com.dlong.rep.dleventbus.model.DLSubscription;

import java.lang.reflect.InvocationTargetException;

/**
 * Created by vimerzhao on 18-12-23
 */
public class DefaultEventHandler implements IEventHandler {
    @Override
    public void handleEvent(DLSubscription subscription, Object message) {
        if (subscription == null || subscription.subscriber == null) {
            return;
        }
        try {
            subscription.subscriberMethod.method.invoke(subscription.subscriber, message);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}
