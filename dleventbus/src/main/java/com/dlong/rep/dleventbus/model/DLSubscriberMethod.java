package com.dlong.rep.dleventbus.model;

import java.lang.reflect.Method;

/**
 * 订阅者模型
 * @author  dlong
 * created at 2019/4/12 9:02 AM
 */
public class DLSubscriberMethod {
    /** 方法 */
    public Method method;
    /** 事件类型 */
    public Class<?> eventType;
    /** 方法名，用来做比较 */
    public String methodString;

    public DLSubscriberMethod(Method method, Class<?> eventType) {
        this.method = method;
        this.eventType = eventType;
    }

    /**
     * 比较方法
     * @param other 比较方
     * @return boolean
     */
    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        } else if (other instanceof DLSubscriberMethod) {
            checkMethodString();
            DLSubscriberMethod otherSubscriberMethod = (DLSubscriberMethod)other;
            otherSubscriberMethod.checkMethodString();
            // Don't use method.equals because of http://code.google.com/p/android/issues/detail?id=7811#c6
            // 不要直接使用 method.equals 这个方法，原因可以查看这个链接
            // http://code.google.com/p/android/issues/detail?id=7811#c6
            // 我看了一下主要是因为耗时较长，性能较差所以不建议使用
            return methodString.equals(otherSubscriberMethod.methodString);
        } else {
            return false;
        }
    }

    /**
     * 获取方法名称
     */
    private synchronized void checkMethodString() {
        if (methodString == null) {
            // Method.toString has more overhead, just take relevant parts of the method
            // Method.toString 需要更多的开销，只需要获取方法的相关部分
            StringBuilder builder = new StringBuilder(64);
            builder.append(method.getDeclaringClass().getName());
            builder.append('#').append(method.getName());
            builder.append('(').append(eventType.getName());
            methodString = builder.toString();
        }
    }

    /**
     * 获得方法的唯一标记码
     * @return int
     */
    @Override
    public int hashCode() {
        return method.hashCode();
    }
}
