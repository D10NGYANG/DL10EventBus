package com.dlong.rep.dleventbus.model;

/**
 * 订阅类-订阅方法 模型
 * @author  dlong
 * created at 2019/4/12 11:57 AM
 */
public class DLSubscription {
    /** 订阅类 */
    final Object subscriber;
    /** 订阅方法 */
    final DLSubscriberMethod subscriberMethod;
    /**
     * 活着的标记
     */
    volatile boolean active;

    public DLSubscription(Object subscriber, DLSubscriberMethod subscriberMethod) {
        this.subscriber = subscriber;
        this.subscriberMethod = subscriberMethod;
        active = true;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof DLSubscription) {
            DLSubscription otherSubscription = (DLSubscription) other;
            return subscriber == otherSubscription.subscriber
                    && subscriberMethod.equals(otherSubscription.subscriberMethod);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return subscriber.hashCode() + subscriberMethod.methodString.hashCode();
    }
}
