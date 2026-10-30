package com.bioinformatics.common.models;

import java.util.*;

public class SearchRequestUtils {

    /**
     * Formats a non-null, non-empty field for diagnostic output.
     *
     * @param fieldName field name to display
     * @param value     field value
     * @return formatted {@code name=value} text with a trailing separator, or an empty string
     * when the value is null or an empty list
     */
    public static String fieldToString(String fieldName, Object value) {
        if (value == null) return "";
        if (value instanceof List<?> lst && lst.isEmpty()) return "";
        return String.format("%s=%s, ", fieldName, value);
    }

    /**
     * Formats the populated pagination fields for diagnostic output.
     *
     * @return formatted pagination-field text
     */
    public static String PaginationToString(Integer page,
                                            Integer size,
                                            String sort,
                                            String direction) {
        String sb = "Pagination=[" + fieldToString("page", page) +
                fieldToString("size", size) +
                fieldToString("sort", sort) +
                fieldToString("direction", direction);
        var s = sb.trim();
        if (s.endsWith(",")) {
            s = s.substring(0, s.length() - 1);
        }
        return s + "]";
    }

    public static <T> List<T> safeListCopy(List<T> origin) {
        return (origin != null) ? new ArrayList<>(origin) : new ArrayList<>();
    }

    public static <K, V> Map<K, List<V>> safeMapCopy(Map<K, List<V>> origin) {
        if (origin == null) {
            return new HashMap<>();
        }

        var copyMap = new HashMap<K, List<V>>();
        origin.forEach((key, value) -> copyMap.put(key, safeListCopy(value)));

        return copyMap;
    }

    /**
     * Compares two lists as null-safe sets, ignoring order and duplicates.
     *
     * @param listA first list
     * @param listB second list
     * @param <T> element type
     * @return {@code true} if both lists are the same reference or contain the same distinct elements
     */
    public static <T> boolean listEquals(List<T> listA, List<T> listB) {
        if (listA == listB) return true;
        if (listA == null || listB == null) return false;

        var setA = new HashSet<T>(listA);
        var setB = new HashSet<T>(listB);

        return setA.equals(setB);
    }
}
