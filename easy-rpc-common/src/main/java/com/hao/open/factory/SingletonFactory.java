package com.hao.open.factory;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 单例工厂
 */
public class SingletonFactory {

    private static final Map<String, Object> OBJECT_MAP = new ConcurrentHashMap<>();

    private SingletonFactory() {

    }

    public static <T> T getInstance(Class<T> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException();
        }
        String key = clazz.toString();
        if (OBJECT_MAP.containsKey(key)) {
            return clazz.cast(OBJECT_MAP.get(key));
        } else {
            return clazz.cast(OBJECT_MAP.computeIfAbsent(key, k -> {
                try {
                    return clazz.getDeclaredConstructor().newInstance();
                } catch (InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
            }));
        }
    }

}
