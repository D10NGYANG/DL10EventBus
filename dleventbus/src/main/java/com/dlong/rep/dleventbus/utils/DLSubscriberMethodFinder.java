package com.dlong.rep.dleventbus.utils;

import com.dlong.rep.dleventbus.DLSubscribe;
import com.dlong.rep.dleventbus.exception.DLEventBusException;
import com.dlong.rep.dleventbus.model.DLSubscriberMethod;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 订阅者注册的事件监听方法查找工具
 * -------------------------------------
 * 用于查找订阅者注册的方法
 * -------------------------------------
 * @author  dlong
 * created at 2019/4/12 9:21 AM
 */
public class DLSubscriberMethodFinder {

    // 声明实例
    private static DLSubscriberMethodFinder instance;

    private static final int BRIDGE = 0x40;
    private static final int SYNTHETIC = 0x1000;
    // 筛选，忽视的修饰词
    private static final int MODIFIERS_IGNORE = Modifier.ABSTRACT | Modifier.STATIC | BRIDGE | SYNTHETIC;

    // 这里使用 ConcurrentHashMap 是为了线程安全，分段锁的形式保证操作该部分数据时只有一个任务进入
    // 详细说明查看链接
    // https://blog.csdn.net/jjc120074203/article/details/78625433
    // 这是一个缓存空间，将注册过的订阅者信息保存起来；当再次注册的时候不再需要去查找，节省时间
    private static final Map<Class<?>, List<DLSubscriberMethod>> methodCatch = new ConcurrentHashMap<>();

    /**
     * 获取实例
     * @return DLSubscriberMethodFinder
     */
    public static DLSubscriberMethodFinder getInstance(){
        if (instance == null){
            synchronized (DLSubscriberMethodFinder.class){
                if (instance == null){
                    instance = new DLSubscriberMethodFinder();
                }
            }
        }
        return instance;
    }

    /**
     * 查找
     * @param subscriberClass 订阅者class
     * @return List<DLSubscriberMethod>
     */
    public List<DLSubscriberMethod> findSubscriberMethods(Class<?> subscriberClass) {
        List<DLSubscriberMethod> subscriberMethods = methodCatch.get(subscriberClass);
        if (subscriberMethods != null) {
            return subscriberMethods;
        }

        subscriberMethods = findUsingReflection(subscriberClass);

        if (subscriberMethods.isEmpty()) {
            // 抛出没有找到注册的监听方法的异常
            throw new DLEventBusException("Subscriber " + subscriberClass
                    + " and its super classes have no public methods with the @Subscribe annotation");
        } else {
            methodCatch.put(subscriberClass, subscriberMethods);
            return subscriberMethods;
        }
    }

    /**
     * 查找反射方法
     * @param subscriberClass 类
     * @return 列表
     */
    private List<DLSubscriberMethod> findUsingReflection(Class<?> subscriberClass) {
        List<DLSubscriberMethod> methodList = new ArrayList<>();
        // 根据参数类型来保存监听方法
        Map<Class, Object> anyMethodByEventType = new HashMap<>();
        if (subscriberClass == null) return methodList;
        Method[] methods;
        try {
            // This is faster than getMethods, especially when subscribers are fat classes like Activities
            // 这比 getMethods 方法快不少，特别是在订阅者是活动这样的类的时候
            methods = subscriberClass.getDeclaredMethods();
        } catch (Throwable th) {
            // Workaround for java.lang.NoClassDefFoundError, see https://github.com/greenrobot/EventBus/issues/149
            // 没有找到时再使用 getMethods 方法
            methods = subscriberClass.getMethods();
        }
        for (Method method : methods) {
            int modifiers = method.getModifiers();
            if ((modifiers & Modifier.PUBLIC) != 0 && (modifiers & MODIFIERS_IGNORE) == 0){
                // 获取方法的传入参数类型
                Class<?>[] parameterTypes = method.getParameterTypes();
                // 找到参数只有一个的方法
                if (parameterTypes.length == 1) {
                    // 判断是否带有我规定的注解
                    DLSubscribe subscribeAnnotation = method.getAnnotation(DLSubscribe.class);
                    if (subscribeAnnotation != null) {
                        // 有注解，并拿到参数类型
                        Class<?> eventType = parameterTypes[0];
                        // 每个类里面带有同一种参数类型的注解监听只能有一个
                        // 所以我用一个 Map 来记录，key 就是参数类型
                        if (anyMethodByEventType.get(eventType) == null) {
                            anyMethodByEventType.put(eventType, method);
                            methodList.add(new DLSubscriberMethod(method, eventType));
                        }
                    }
                } else if (method.isAnnotationPresent(DLSubscribe.class)) {
                    // 如果带有我规定的注解，但是参数多于1个就报错
                    String methodName = method.getDeclaringClass().getName() + "." + method.getName();
                    throw new DLEventBusException("@Subscribe method " + methodName +
                            "must have exactly 1 parameter but has " + parameterTypes.length);
                }
            }
        }
        return methodList;
    }
}
