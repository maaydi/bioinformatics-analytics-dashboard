package com.bioinformatics.common.config.cache;

import com.bioinformatics.shared.models.cache.TypedCacheSpec;

import java.util.List;

public interface CacheRegistryProvider {
    List<TypedCacheSpec> getCacheSpecs();
}
