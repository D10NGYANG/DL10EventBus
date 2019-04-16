package com.dlong.rep.dleventbus.model;

/**
 * 事件类型
 * @author  dlong
 * created at 2019/4/16 10:11 AM
 */
public class DLEventType {
    public Class<?> paramType;

    public DLEventType(Class<?> paramType) {
        this.paramType = paramType;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((paramType== null) ? 0 : paramType.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DLEventType) {
            DLEventType other = (DLEventType) obj;
            return paramType.equals(other.paramType);
        } else {
            return false;
        }
    }
}
