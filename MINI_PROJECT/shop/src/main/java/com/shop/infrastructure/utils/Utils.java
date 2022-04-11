package com.shop.infrastructure.utils;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class Utils {
    public static  <T> boolean isNullOrEmpty(T t) {
        if (t instanceof Map) {
            return ((Map<?, ?>) t).isEmpty();
        }
        if (t instanceof List) {
            return ((List<?>) t).isEmpty();
        }
        if (t instanceof Set) {
            return ((Set<?>) t).isEmpty();
        }
        return t == null;
    }

    public static  <T> boolean isNotNullOrEmpty(T t) {
        if (t instanceof Map) {
            return !((Map<?, ?>) t).isEmpty();
        }
        if (t instanceof List) {
            return !((List<?>) t).isEmpty();
        }
        if (t instanceof Set) {
            return !((Set<?>) t).isEmpty();
        }
        return t != null;
    }

    private Utils(){}
}
