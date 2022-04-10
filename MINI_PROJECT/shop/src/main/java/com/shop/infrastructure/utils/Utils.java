package com.shop.infrastructure.utils;

import java.util.List;

public class Utils {
    public static <T> boolean  isNullOrEmptyList(List<T> list) {
        return list == null || list.isEmpty();
    }

    private Utils(){}
}
