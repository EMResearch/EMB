package no.nav.tag.tiltaksgjennomforing.infrastruktur.cache;


import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.PersistenceConfiguration;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.ehcache.EhCacheCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

import static net.sf.ehcache.config.PersistenceConfiguration.Strategy.NONE;
import static net.sf.ehcache.store.MemoryStoreEvictionPolicy.LRU;

@Configuration
@EnableCaching
public class EhCacheConfig extends CachingConfigurerSupport {

    public final static String ABAC_CACHE = "abac_cache";

    public static final String AXSYS_CACHE = "axsys_cache";

    public static final String PDL_CACHE = "pdl_cache";

    public static final String NORGNAVN_CACHE = "norgnavn_cache";

    public static final String NORG_GEO_ENHET = "norggeoenhet_cache";

    public static final String ARENA_CACHCE = "arena_cache";

    @Bean
    public CacheManager ehCacheManager(CacheDto cacheDto) {
        net.sf.ehcache.config.Configuration config = new net.sf.ehcache.config.Configuration();
        cacheDto.getEhcaches().forEach(cache -> config.addCache(
                setupCache(
                        cache.getName(),
                        cache.getMaximumSize(),
                        Duration.ofMinutes(cache.getExpiryInMinutes())
                )
        ));
        return CacheManager.newInstance(config);
    }

    @Bean
    public EhCacheCacheManager cacheManager(CacheDto cacheDto) {
        return new EhCacheCacheManager(ehCacheManager(cacheDto));
    }

    private static CacheConfiguration setupCache(String name, int maxEntriesLocalHeap, Duration duration) {
        return new CacheConfiguration(name, maxEntriesLocalHeap)
                .memoryStoreEvictionPolicy(LRU)
                .timeToIdleSeconds(duration.getSeconds())
                .timeToLiveSeconds(duration.getSeconds())
                .persistence(new PersistenceConfiguration().strategy(NONE));
    }
}
