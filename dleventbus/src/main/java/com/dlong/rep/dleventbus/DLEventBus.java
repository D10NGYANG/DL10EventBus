package com.dlong.rep.dleventbus;

import com.dlong.rep.dleventbus.exception.DLEventBusException;
import com.dlong.rep.dleventbus.handler.AsyncEventHandler;
import com.dlong.rep.dleventbus.handler.IEventHandler;
import com.dlong.rep.dleventbus.model.DLEventType;
import com.dlong.rep.dleventbus.model.DLSubscriberMethod;
import com.dlong.rep.dleventbus.model.DLSubscription;
import com.dlong.rep.dleventbus.utils.DLSubscriberMethodFinder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

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

    /** 发布事件线程池 */
    private ThreadLocal<Queue<Class<?>>> mThreadLocalEvents = new ThreadLocal<Queue<Class<?>>>() {
        @Override
        protected Queue<Class<?>> initialValue() {
            return new ConcurrentLinkedQueue<>();
        }
    };

    /** 异步发布工具 */
    private EventDispatcher mEventDispatcher = new EventDispatcher();

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
        }
    }

    /**
     * 删除某类的监听事件
     * @param subscriber
     * @param eventType
     */
    private void unsubscribeByEventType(Object subscriber, Class<?> eventType) {
        List<DLSubscription> subscriptions = subscriptionsByEventType.get(eventType);
        if (subscriptions != null) {
            int size = subscriptions.size();
            for (int i = 0; i < size; i++) {
                DLSubscription subscription = subscriptions.get(i);
                if (subscription.subscriber == subscriber) {
                    subscription.active = false;
                    subscriptions.remove(i);
                    i--;
                    size--;
                }
            }
        }
    }

    /**
     * 发布事件消息
     * @param event 事件
     */
    public void post(Object event) {
        if (event == null) {
            return;
        }
        Objects.requireNonNull(mThreadLocalEvents.get()).offer(event.getClass());
        mEventDispatcher.dispatchEvents(event);
    }

    private class EventDispatcher {
        private IEventHandler mAsyncEventHandler = new AsyncEventHandler();

        void dispatchEvents(Object event) {
            Queue<Class<?>> eventQueue = mThreadLocalEvents.get();
            if (null == eventQueue) return;
            while (eventQueue.size() > 0) {
                handleEvent(eventQueue.poll(),event);
            }
        }

        private void handleEvent(Class<?> cla, Object event) {
            List<DLSubscription> subscriptions = subscriptionsByEventType.get(cla);
            if (subscriptions == null) {
                return;
            }
            for (DLSubscription subscription : subscriptions) {
                IEventHandler eventHandler = mAsyncEventHandler;
                eventHandler.handleEvent(subscription, event);
            }
        }
    }
}
