package no.nav.familie.ba.sak.common

import org.springframework.cache.CacheManager

fun CacheManager.clearAllCaches() = this.cacheNames.forEach { getCache(it)?.clear() }
