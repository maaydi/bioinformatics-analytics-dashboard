package com.bioinformatics.shared.models.cache;

public record TypedCacheSpec(
        Class<?> parameterizedType,
        Class<?> elementType,
        String... cacheNames
) {
}
