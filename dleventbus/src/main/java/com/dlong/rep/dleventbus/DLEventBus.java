package com.dlong.rep.dleventbus;

import com.dlong.rep.dleventbus.exception.DLEventBusException;
import com.dlong.rep.dleventbus.model.DLSubscriberMethod;
import com.dlong.rep.dleventbus.model.DLSubscription;
import com.dlong.rep.dleventbus.utils.DLSubscriberMethodFinder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

/**
 * 事件总线
 * -----------------------------
 * 1、注册-类；
 * 1.1、订阅-方法；
 * 2、反注册；
 * 3、post消息
 * -----------------------------
 * @author  dlong
 * created at 2019/4/11 4:50 PM
 */
public class DLEventBus {

    /** 定义一个默认实例 */
    private static volatile DLEventBus defaultInstance;

    /** 每个事件类型带有订阅者 */
    // CopyOnWriteArrayList 线程安全列表 原理介绍：https://blog.csdn.net/hua631150873/article/details/51306021
    private final Map<Class<?>, CopyOnWriteArrayList<DLSubscription>> subscriptionsByEventType = new HashMap<>();
    /** 每个订阅者带有订阅事件总类 */
    private final Map<Object, List<Class<?>>> typesBySubscriber = new HashMap<>();

    /** 得到事件总线实例 */
    public static DLEventBus getDefault() {
        // 判断是否实例化
        if (defaultInstance == null) {
            // 锁定只能通过一个任务
            synchronized (DLEventBus.class) {
                if (defaultInstance == null) {
                    // 实例化
                    defaultInstance = new DLEventBus();
                }
            }
        }
        return defaultInstance;
    }

    /**
     * 注册类
     * @param subscriber 类
     */
    public void register(Object subscriber) {
        Class<?> subscriberClass = subscriber.getClass();
        List<DLSubscriberMethod> subscriberMethods = DLSubscriberMethodFinder
                .getInstance().findSubscriberMethods(subscriberClass);
        synchronized (this) {
            for (DLSubscriberMethod subscriberMethod : subscriberMethods) {
                subscribe(subscriber, subscriberMethod);
            }
        }
    }

    /**
     * 订阅方法
     * Must be called in synchronized block
     * @param subscriber 类
     * @param subscriberMethod 方法
     */
    private void subscribe(Object subscriber, DLSubscriberMethod subscriberMethod) {
        Class<?> eventType = subscriberMethod.eventType;
        DLSubscription newSubscription = new DLSubscription(subscriber, subscriberMethod);
        // 查记录列表，看有没有这个事件类型的订阅者
        CopyOnWriteArrayList<DLSubscription> subscriptions = subscriptionsByEventType.get(eventType);
        if (subscriptions == null) {
            // 没有对应的订阅者
            subscriptions = new CopyOnWriteArrayList<>();
        } else {
            // 有了对应的订阅者，再判断是否存在相同的订阅者，有就报错
            if (subscriptions.contains(newSubscription)) {
                throw new DLEventBusException("Subscriber " + subscriber.getClass() + " already registered to event "
                        + eventType);
            }
        }
        subscriptions.add(newSubscription);
        // 搞一个订阅者怼进去
        subscriptionsByEventType.put(eventType, subscriptions);
        // 查记录列表，看有没有记录过这个订阅者的订阅事件
        List<Class<?>> subscribedEvents = typesBySubscriber.get(subscriber);
        if (subscribedEvents == null) {
            subscribedEvents = new ArrayList<>();
        }
        subscribedEvents.add(eventType);
        // 没有记录过，就又怼一个进去
        typesBySubscriber.put(subscriber, subscribedEvents);
    }

    /**
     * 判断这个订阅者是否在订阅中
     * @param subscriber 类
     * @return
     */
    public synchronized boolean isRegistered(Object subscriber) {
        return typesBySubscriber.containsKey(subscriber);
    }

    /**
     * 反注册
     * @param subscriber 类
     */
    public synchronized void unregister(Object subscriber) {
        List<Class<?>> subscribedTypes = typesBySubscriber.get(subscriber);
        if (subscribedTypes != null) {
            for (Class<?> eventType : subscribedTypes) {
                unsubscribeByEventType(subscriber, eventType);
            }
            typesBySubscriber.remove(subscriber);
        } else {
            logger.log(Level.WARNING, "Subscriber to unregister was not registered before: " + subscriber.getClass());
        }
    }
}
