package com.reddit.clone.service.cachecleaner;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CacheCleaner {

    private final CacheManager cacheManager;

    public CacheCleaner(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public void clearAllCaches() {
        log.info("Clearing all caches...");

        cacheManager.getCacheNames().forEach(name -> {
            if (cacheManager.getCache(name) != null) {
                cacheManager.getCache(name).clear();
                log.info("Cleared cache: {}", name);
            }
        });

        log.info("✅ All caches cleared successfully");
    }
}
